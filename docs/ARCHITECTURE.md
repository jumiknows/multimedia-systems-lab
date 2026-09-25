# Architecture

## Audio

`WavReader`
Reads RIFF chunks, validates PCM audio and separates the channels.

`WaveformPanel`
Reduces sample arrays to display-sized peaks without changing the source data.

`HuffmanCodec`
Counts symbols, builds the Huffman tree, packs encoded bits into bytes and decodes them again.

The UI reports compression statistics only after the decoded samples match the original input.

## Images

`ImageStudioPanel`
Loads the image and runs each transform on a copy.

`ImageTransforms`
Handles grayscale, YUV edits, colour mapping and dithering.

`DctCodec`
Processes eight by eight blocks and reports PSNR after reconstruction.

`FourierTransform`
Runs a two-dimensional FFT and renders the logarithmic magnitude spectrum.

## UI

`MultimediaLabApp` hosts the audio and image tools in one window.

Long-running image work uses a background worker so the interface remains responsive.
