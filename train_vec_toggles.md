# train_vec.py toggles and integration guide

`train_vec.py` trains fast token embeddings from local token neighborhoods, saves them to disk, and exposes APIs to use vectors as a pre/post layer around another model.

## Main training arguments

- `--input <files...>`: one or more text files to train from.
- `--max-tokens N`: stop after processing N tokens.
- `--dim D`: embedding size (`8`, `16`, etc.).
- `--window W`: local neighborhood radius on each side.
- `--epochs E`: training passes over token stream.
- `--lr LR`: SGD learning rate.
- `--negatives K`: negative samples per positive pair.
- `--min-count C`: drop rare tokens below count C.
- `--tokenizer {word,char}`: word/punctuation tokens or character tokens.
- `--seed S`: deterministic random seed.
- `--save PREFIX`: output paths `PREFIX.json` and `PREFIX.npz`.

## Experiment toggles

- `--use-attention`:
  - Enables a tiny attention-style weighting over nearby context tokens.
  - If off, context uses uniform averaging.
- `--attention-temp T`:
  - Temperature for attention sharpness.
  - Lower values (`~0.5`) produce peaky weights; higher values (`~2.0`) smoother.
- `--examples N`:
  - Number of random nearest-neighbor examples printed after training.
- `--generate-steps N`:
  - Number of tokens in generation demo.

## Metrics printed after training

- `avg_neg_sampling_loss`: average training objective (lower is better).
- `adjacent_cosine`: cosine similarity for adjacent tokens in stream.
- `random_cosine`: cosine similarity for random token pairs.
- `next_recall@5`: recall@5 of predicting next token from left context average.

Good signs:
- `adjacent_cosine > random_cosine`.
- `next_recall@5` increases when tuning `dim/window/epochs`.
- meaningful nearest-neighbor examples.

## Saved model format

- `PREFIX.json`: config and vocabulary.
- `PREFIX.npz`: `input_emb` and `output_emb` arrays.

Load with:

```python
from train_vec import VecEmbeddingModel
model = VecEmbeddingModel.load("artifacts/vec_model")
```

## Use as pre-step before another neural model

Example: convert token sequence to context-aware vectors before feeding a baseline network.

```python
tokens = ["the", "cat", "sat", "on", "the", "mat"]
x_vec = model.encode_tokens(tokens)  # shape: [seq_len, dim]
# feed x_vec to your model instead of one-hot/token-id embedding lookup
```

## Use as post-step after another neural model

If your model outputs vectors in the same `dim`, decode back to token candidates:

```python
pred_vecs = your_model_output  # shape: [seq_len, dim]
ranked = model.decode_vectors(pred_vecs, top_k=3)
# ranked[t] -> [(token, score), ...]
```

## Optional quick generation

`VecEmbeddingModel.generate(prompt, steps=...)` does simple nearest-neighbor sampling in embedding space, useful for sanity checks and demos.
