# Multimedia Systems Lab

Two Java desktop tools for exploring audio, image processing and compression algorithms.

The project is an original reimplementation based on concepts from SFU CMPT 365. It does not include course starter code, grading data or original submission material.

## What is included

### Audio Explorer

- PCM WAV parsing
- stereo waveform display
- Huffman tree construction
- real bit packing and decoding
- entropy and compression statistics

### Image Studio

- grayscale conversion
- YUV brightness and saturation controls
- monochrome colour mapping
- ordered dithering
- Floyd Steinberg dithering
- block DCT compression
- PSNR measurement
- Fourier magnitude spectrum

## Run it

Requires JDK 17 or newer.

macOS or Linux:

```bash
./scripts/build.sh
./scripts/run.sh
```

Windows PowerShell:

```powershell
./scripts/build.ps1
./scripts/run.ps1
```

The application opens with generated demo media, so no sample files are required.

## Screenshots

![Audio Explorer](docs/screenshots/audio-explorer.png)

![Image Studio](docs/screenshots/image-studio.png)

## Test it

macOS or Linux:

```bash
./scripts/test.sh
```

Windows PowerShell:

```powershell
./scripts/test.ps1
```

The tests cover WAV parsing, Huffman round trips, YUV behaviour, colour mapping, dithering, DCT output and Fourier sizing.

## Code map

`src/main/java/ca/ernestwong/multimedia/audio`
WAV parsing, Huffman coding and waveform UI.

`src/main/java/ca/ernestwong/multimedia/image`
Image transforms, DCT, Fourier processing and image UI.

`src/main/java/ca/ernestwong/multimedia/demo`
Generated sample media.

`src/test/java`
Algorithm tests.

## Notes

- WAV input supports uncompressed PCM with 8 or 16 bit samples and up to two channels.
- Huffman results measure the sample stream only. A real file format would also store the codebook and metadata.
- The DCT tool demonstrates transform and quantization stages. It does not write JPEG files.
- Monochrome colour mapping is an artistic transform, not semantic colour recovery.
- Large images are resized to a manageable power of two before Fourier processing.

See [Architecture](docs/ARCHITECTURE.md) for the data flow and [Algorithms](docs/ALGORITHMS.md) for concise notes on the implemented methods.

## License

MIT
