# Architecture

## Audio path

1. `WavReader` scans RIFF chunks and validates the PCM format.
2. Interleaved frames are converted into one integer array per channel.
3. `WaveformPanel` reduces the arrays to screen sized peaks without changing the source data.
4. `HuffmanCodec` counts symbols, builds the binary tree, and assigns a bit code to each sample value.
5. The selected channel is packed into bytes and decoded through the same tree.
6. The UI reports compression statistics only after the decoded samples match the input.

## Image path

1. `ImageStudioPanel` loads the image into an RGB buffer.
2. The selected operation runs on a copy so the original remains available.
3. YUV operations convert RGB pixels to luma and chroma, modify only the requested components, then convert back to RGB.
4. Monochrome colourization maps luminance between selectable shadow and highlight colours before blending the result.
5. Ordered dithering compares each grayscale pixel with a repeating threshold matrix.
6. Floyd Steinberg dithering carries quantization error into neighbouring pixels.
7. DCT compression processes eight by eight blocks for red, green, and blue independently.
8. Fourier processing converts grayscale rows and columns with a radix two FFT and renders logarithmic magnitude.

## User interface

`MultimediaLabApp` hosts both tools in one window. Each panel owns its own state and can also be launched separately for focused demonstrations.

Long running image transforms use a background worker so the interface stays responsive.
