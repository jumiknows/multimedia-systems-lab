# Algorithms

This file is a short reference for the main algorithms implemented in the project.

## PCM and WAV

PCM stores an amplitude value for every sample in time.

A stereo WAV file interleaves left and right channel samples. The reader scans RIFF chunks instead of assuming the audio data begins at a fixed offset, because valid WAV files can contain optional metadata chunks.

## Huffman coding

Huffman coding gives shorter bit patterns to common symbols and longer patterns to rare symbols.

The implementation:

1. counts each sample value
2. creates one tree node per symbol
3. repeatedly combines the two least frequent nodes
4. assigns bits by walking the tree
5. packs the result into bytes
6. decodes through the same tree
7. verifies that every recovered sample matches the input

The codes are prefix free because symbols only appear at leaf nodes.

## Dithering

Ordered dithering compares pixels with a repeating threshold matrix.

Floyd Steinberg dithering makes a black or white decision at each pixel and distributes the error to neighbouring pixels that have not been processed yet.

## YUV

YUV separates brightness from colour information.

The brightness control changes Y while leaving U and V alone.

The saturation control keeps Y and scales U and V.

Clipping and rounding can introduce small differences when converting back to RGB.

## Monochrome colour mapping

The tool maps dark pixels toward a selected shadow colour and bright pixels toward a selected highlight colour.

It is a visual effect. It does not infer the original colour of an object.

## DCT

The discrete cosine transform expresses an eight by eight image block as spatial frequencies.

Quantization removes detail by reducing small high-frequency coefficients toward zero. The inverse transform reconstructs the visible image.

PSNR compares the reconstructed image with the source. It is useful for numerical comparison but does not perfectly represent perceived image quality.

## Fourier transform

The two-dimensional Fourier transform is computed by processing rows and columns.

The displayed spectrum uses a logarithmic scale because low-frequency values are usually much larger than the rest.
