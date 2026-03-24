#!/usr/bin/env python3
"""Train lightweight context-aware token vectors.

Design goals:
- Very fast training on local token neighborhoods.
- Save/load model for use in other pipelines.
- Optional tiny attention block over nearby tokens.
- Print metrics and qualitative examples.
"""

from __future__ import annotations

import argparse
import json
import math
import random
import re
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Sequence, Tuple

import numpy as np


_TOKEN_RE = re.compile(r"\w+|[^\w\s]", re.UNICODE)


@dataclass
class VecConfig:
    dim: int = 16
    window: int = 3
    negatives: int = 5
    lr: float = 0.05
    epochs: int = 2
    max_tokens: int = 200_000
    min_count: int = 1
    seed: int = 42
    tokenizer: str = "word"
    use_attention: bool = False
    attention_temp: float = 1.0


class VecEmbeddingModel:
    """Skip-gram embeddings with optional context-attention view helpers."""

    def __init__(self, config: VecConfig):
        self.config = config
        self.token_to_id: Dict[str, int] = {}
        self.id_to_token: List[str] = []
        self.input_emb: np.ndarray | None = None
        self.output_emb: np.ndarray | None = None
        self.unigram_cdf: np.ndarray | None = None
        self.rng = np.random.default_rng(config.seed)

    # ---------- Data ----------
    def tokenize(self, text: str) -> List[str]:
        if self.config.tokenizer == "char":
            return list(text)
        return _TOKEN_RE.findall(text.lower())

    def build_vocab(self, tokens: Sequence[str]) -> List[int]:
        counts = Counter(tokens)
        vocab = [tok for tok, c in counts.items() if c >= self.config.min_count]
        vocab.sort(key=lambda t: (-counts[t], t))
        self.token_to_id = {tok: i for i, tok in enumerate(vocab)}
        self.id_to_token = vocab
        ids = [self.token_to_id[t] for t in tokens if t in self.token_to_id]

        n = len(vocab)
        if n == 0:
            raise ValueError("Vocabulary is empty. Lower --min-count or provide more data.")
        scale = 0.5 / self.config.dim
        self.input_emb = self.rng.normal(0.0, scale, size=(n, self.config.dim)).astype(np.float32)
        self.output_emb = np.zeros((n, self.config.dim), dtype=np.float32)

        # Negative sampling distribution ~ count^0.75
        freqs = np.array([counts[t] for t in vocab], dtype=np.float64)
        dist = np.power(freqs, 0.75)
        dist /= dist.sum()
        self.unigram_cdf = np.cumsum(dist)
        return ids

    # ---------- Core training ----------
    def _sample_negative(self, k: int, forbidden: int) -> np.ndarray:
        assert self.unigram_cdf is not None
        out = np.empty(k, dtype=np.int32)
        i = 0
        while i < k:
            r = self.rng.random()
            idx = int(np.searchsorted(self.unigram_cdf, r, side="left"))
            if idx != forbidden:
                out[i] = idx
                i += 1
        return out

    @staticmethod
    def _sigmoid(x: np.ndarray | float) -> np.ndarray | float:
        return 1.0 / (1.0 + np.exp(-x))

    def _attention_weights(self, center_vec: np.ndarray, context_ids: np.ndarray) -> np.ndarray:
        assert self.input_emb is not None
        ctx = self.input_emb[context_ids]
        scores = (ctx @ center_vec) / max(1e-6, self.config.attention_temp * math.sqrt(self.config.dim))
        scores = scores - np.max(scores)
        w = np.exp(scores)
        w /= np.sum(w)
        return w.astype(np.float32)

    def train_ids(self, ids: Sequence[int]) -> Dict[str, float]:
        assert self.input_emb is not None and self.output_emb is not None
        n = len(ids)
        if n < 3:
            raise ValueError("Need at least 3 token ids to train.")

        total_loss = 0.0
        pair_count = 0
        window = self.config.window

        id_array = np.array(ids, dtype=np.int32)

        for _epoch in range(self.config.epochs):
            for i in range(n):
                c = id_array[i]
                left = max(0, i - window)
                right = min(n, i + window + 1)
                context = np.concatenate((id_array[left:i], id_array[i + 1 : right]))
                if context.size == 0:
                    continue

                center_vec = self.input_emb[c].copy()
                if self.config.use_attention:
                    weights = self._attention_weights(center_vec, context)
                else:
                    weights = np.full(context.shape[0], 1.0 / context.shape[0], dtype=np.float32)

                for w, pos in zip(weights, context):
                    pos_out = self.output_emb[pos]
                    pos_score = float(center_vec @ pos_out)
                    pos_prob = float(self._sigmoid(pos_score))
                    pos_grad = (1.0 - pos_prob) * self.config.lr * float(w)

                    neg_ids = self._sample_negative(self.config.negatives, forbidden=pos)
                    neg_out = self.output_emb[neg_ids]
                    neg_scores = neg_out @ center_vec
                    neg_probs = self._sigmoid(neg_scores)
                    neg_grads = (-neg_probs * self.config.lr * float(w)).astype(np.float32)

                    # update output vectors
                    self.output_emb[pos] += pos_grad * center_vec
                    self.output_emb[neg_ids] += neg_grads[:, None] * center_vec[None, :]

                    # update center vector (accumulate from all outputs)
                    grad_center = pos_grad * pos_out + np.sum(neg_grads[:, None] * neg_out, axis=0)
                    self.input_emb[c] += grad_center
                    center_vec = self.input_emb[c]

                    # loss for reporting
                    total_loss += -math.log(max(pos_prob, 1e-8))
                    total_loss += -float(np.sum(np.log(np.maximum(1.0 - neg_probs, 1e-8))))
                    pair_count += 1

        avg_loss = total_loss / max(pair_count, 1)
        metrics = self.evaluate_ids(id_array)
        metrics["avg_neg_sampling_loss"] = avg_loss
        metrics["pairs"] = float(pair_count)
        return metrics

    # ---------- Embedding API for downstream models ----------
    def token_embedding_matrix(self, normalized: bool = True) -> np.ndarray:
        assert self.input_emb is not None and self.output_emb is not None
        mat = (self.input_emb + self.output_emb) * 0.5
        if normalized:
            norms = np.linalg.norm(mat, axis=1, keepdims=True) + 1e-9
            mat = mat / norms
        return mat.astype(np.float32)

    def encode_tokens(self, tokens: Sequence[str], context_window: int | None = None) -> np.ndarray:
        """Context-aware vectors for a token sequence, ready for another model input."""
        mat = self.token_embedding_matrix(normalized=True)
        w = context_window or self.config.window
        unk = np.zeros(self.config.dim, dtype=np.float32)
        ids = [self.token_to_id.get(t, -1) for t in tokens]
        out = np.zeros((len(tokens), self.config.dim), dtype=np.float32)
        for i, tid in enumerate(ids):
            if tid < 0:
                out[i] = unk
                continue
            left = max(0, i - w)
            right = min(len(ids), i + w + 1)
            ctx = [x for j, x in enumerate(ids[left:right], start=left) if x >= 0 and j != i]
            if not ctx:
                out[i] = mat[tid]
                continue
            if self.config.use_attention:
                center = mat[tid]
                ctx_arr = np.array(ctx, dtype=np.int32)
                scores = mat[ctx_arr] @ center
                scores = scores / max(1e-6, self.config.attention_temp)
                scores = scores - np.max(scores)
                weights = np.exp(scores)
                weights /= np.sum(weights)
                ctx_vec = weights @ mat[ctx_arr]
            else:
                ctx_vec = np.mean(mat[np.array(ctx, dtype=np.int32)], axis=0)
            out[i] = 0.5 * mat[tid] + 0.5 * ctx_vec
        return out

    def decode_vectors(self, vectors: np.ndarray, top_k: int = 1) -> List[List[Tuple[str, float]]]:
        """Map vectors back to nearest tokens (post-step after a model outputs vectors)."""
        mat = self.token_embedding_matrix(normalized=True)
        v = vectors.astype(np.float32)
        v /= np.linalg.norm(v, axis=1, keepdims=True) + 1e-9
        sims = v @ mat.T
        top_idx = np.argpartition(-sims, kth=min(top_k, sims.shape[1] - 1), axis=1)[:, :top_k]
        results: List[List[Tuple[str, float]]] = []
        for row, idxs in enumerate(top_idx):
            ranked = sorted(((int(i), float(sims[row, i])) for i in idxs), key=lambda x: -x[1])
            results.append([(self.id_to_token[i], s) for i, s in ranked])
        return results

    # ---------- Evaluation & qualitative checks ----------
    def evaluate_ids(self, ids: np.ndarray, eval_samples: int = 2000, recall_k: int = 5) -> Dict[str, float]:
        mat = self.token_embedding_matrix(normalized=True)
        n = len(ids)
        if n < 4:
            return {"adjacent_cosine": 0.0, "random_cosine": 0.0, f"next_recall@{recall_k}": 0.0}

        m = min(eval_samples, n - 1)
        idxs = self.rng.integers(0, n - 1, size=m)
        adj = np.mean(np.sum(mat[ids[idxs]] * mat[ids[idxs + 1]], axis=1))

        ridx1 = self.rng.integers(0, n, size=m)
        ridx2 = self.rng.integers(0, n, size=m)
        rnd = np.mean(np.sum(mat[ids[ridx1]] * mat[ids[ridx2]], axis=1))

        # Predict next token from averaged left context
        correct = 0
        tries = 0
        w = self.config.window
        for i in idxs:
            left = max(0, i - w)
            ctx = ids[left:i]
            if len(ctx) == 0:
                continue
            query = np.mean(mat[ctx], axis=0)
            query /= np.linalg.norm(query) + 1e-9
            sims = mat @ query
            top = np.argpartition(-sims, kth=min(recall_k, len(sims) - 1))[:recall_k]
            target = ids[i + 1]
            correct += int(target in set(int(t) for t in top))
            tries += 1

        rec = correct / max(tries, 1)
        return {
            "adjacent_cosine": float(adj),
            "random_cosine": float(rnd),
            f"next_recall@{recall_k}": float(rec),
        }

    def nearest(self, token: str, k: int = 8) -> List[Tuple[str, float]]:
        if token not in self.token_to_id:
            return []
        mat = self.token_embedding_matrix(normalized=True)
        tid = self.token_to_id[token]
        sims = mat @ mat[tid]
        top = np.argpartition(-sims, kth=min(k + 1, len(sims) - 1))[: k + 1]
        out = sorted(((int(i), float(sims[i])) for i in top if int(i) != tid), key=lambda x: -x[1])[:k]
        return [(self.id_to_token[i], s) for i, s in out]

    def generate(self, prompt: Sequence[str], steps: int = 20, top_k: int = 8, temperature: float = 1.0) -> List[str]:
        """Simple token generation by nearest-neighbor sampling in embedding space."""
        if not self.token_to_id:
            return []
        out = list(prompt)
        mat = self.token_embedding_matrix(normalized=True)
        for _ in range(steps):
            vecs = self.encode_tokens(out[-self.config.window :])
            q = np.mean(vecs, axis=0)
            q /= np.linalg.norm(q) + 1e-9
            sims = mat @ q
            k = min(top_k, len(self.id_to_token))
            idx = np.argpartition(-sims, kth=k - 1)[:k]
            logits = sims[idx] / max(temperature, 1e-6)
            probs = np.exp(logits - np.max(logits))
            probs /= probs.sum()
            nxt = int(self.rng.choice(idx, p=probs))
            out.append(self.id_to_token[nxt])
        return out

    # ---------- Persistence ----------
    def save(self, out_prefix: str) -> None:
        assert self.input_emb is not None and self.output_emb is not None
        prefix = Path(out_prefix)
        prefix.parent.mkdir(parents=True, exist_ok=True)
        meta = {
            "config": vars(self.config),
            "vocab": self.id_to_token,
        }
        with open(prefix.with_suffix(".json"), "w", encoding="utf-8") as f:
            json.dump(meta, f, indent=2)
        np.savez_compressed(
            prefix.with_suffix(".npz"),
            input_emb=self.input_emb,
            output_emb=self.output_emb,
        )

    @classmethod
    def load(cls, out_prefix: str) -> "VecEmbeddingModel":
        prefix = Path(out_prefix)
        with open(prefix.with_suffix(".json"), "r", encoding="utf-8") as f:
            meta = json.load(f)
        cfg = VecConfig(**meta["config"])
        model = cls(cfg)
        model.id_to_token = list(meta["vocab"])
        model.token_to_id = {t: i for i, t in enumerate(model.id_to_token)}
        data = np.load(prefix.with_suffix(".npz"))
        model.input_emb = data["input_emb"].astype(np.float32)
        model.output_emb = data["output_emb"].astype(np.float32)
        return model


def _read_text(paths: Iterable[str]) -> str:
    chunks = []
    for p in paths:
        with open(p, "r", encoding="utf-8", errors="ignore") as f:
            chunks.append(f.read())
    return "\n".join(chunks)


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Train fast context-aware token vectors.")
    p.add_argument("--input", nargs="+", required=True, help="Input text file(s)")
    p.add_argument("--max-tokens", type=int, default=200_000)
    p.add_argument("--dim", type=int, default=16)
    p.add_argument("--window", type=int, default=3)
    p.add_argument("--epochs", type=int, default=2)
    p.add_argument("--lr", type=float, default=0.05)
    p.add_argument("--negatives", type=int, default=5)
    p.add_argument("--min-count", type=int, default=1)
    p.add_argument("--tokenizer", choices=["word", "char"], default="word")
    p.add_argument("--use-attention", action="store_true")
    p.add_argument("--attention-temp", type=float, default=1.0)
    p.add_argument("--seed", type=int, default=42)
    p.add_argument("--save", default="artifacts/vec_model")
    p.add_argument("--examples", type=int, default=6, help="How many nearest-neighbor examples to print")
    p.add_argument("--generate-steps", type=int, default=20)
    return p.parse_args()


def main() -> None:
    args = parse_args()
    cfg = VecConfig(
        dim=args.dim,
        window=args.window,
        negatives=args.negatives,
        lr=args.lr,
        epochs=args.epochs,
        max_tokens=args.max_tokens,
        min_count=args.min_count,
        seed=args.seed,
        tokenizer=args.tokenizer,
        use_attention=args.use_attention,
        attention_temp=args.attention_temp,
    )
    model = VecEmbeddingModel(cfg)

    text = _read_text(args.input)
    tokens = model.tokenize(text)[: cfg.max_tokens]
    ids = model.build_vocab(tokens)

    print(f"Loaded tokens: {len(tokens):,}")
    print(f"Vocabulary size: {len(model.id_to_token):,}")
    print(f"Training config: {cfg}")
    metrics = model.train_ids(ids)

    print("\n=== Metrics ===")
    for k, v in metrics.items():
        print(f"{k:>22}: {v:.6f}")
    if metrics.get("adjacent_cosine", 0) > metrics.get("random_cosine", 0):
        print("Interpretation: neighboring tokens are more similar than random tokens (good).")

    # qualitative neighbors
    print("\n=== Neighbor examples ===")
    sample_tokens = [model.id_to_token[i] for i in model.rng.integers(0, len(model.id_to_token), size=min(args.examples, len(model.id_to_token)))]
    for t in sample_tokens:
        nns = model.nearest(t, k=5)
        joined = ", ".join(f"{w}:{s:.3f}" for w, s in nns)
        print(f"{t:>16} -> {joined}")

    # quick generation demo
    print("\n=== Generate demo ===")
    prompt = tokens[: min(5, len(tokens))] if tokens else [model.id_to_token[0]]
    gen = model.generate(prompt=prompt, steps=args.generate_steps)
    print("prompt:", " ".join(prompt))
    print("output:", " ".join(gen))

    model.save(args.save)
    print(f"\nSaved model to: {args.save}.json and {args.save}.npz")


if __name__ == "__main__":
    main()
