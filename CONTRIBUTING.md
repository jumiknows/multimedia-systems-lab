# Contributing

Keep algorithm changes small and easy to verify.

Before opening a pull request:

```bash
./scripts/build.sh
./scripts/test.sh
```

On Windows, use the matching PowerShell scripts.

Use a focused branch and a clear pull request title such as:

```text
fix: preserve dimensions during DCT processing
test: cover stereo WAV decoding
docs: clarify Huffman compression limits
```

Explain any change that affects algorithm output, numerical behavior, supported media formats, or the desktop UI.

This repository is an original educational reimplementation. Do not add course starter code, assignment text, grading data, or material that should remain private.
