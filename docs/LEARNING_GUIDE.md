# Learning Guide

## PCM audio

PCM stores a measured amplitude for every sample in time. A stereo file interleaves two streams, usually left then right. The sample rate tells us how many frames are captured each second. The bit depth controls how many amplitude values each sample can represent.

The WAV reader does not assume the audio data starts at a fixed byte position. It scans RIFF chunks until it finds both the format description and sample data. This matters because valid WAV files can contain optional metadata chunks.

## Huffman coding

Huffman coding gives short bit patterns to common symbols and longer patterns to rare symbols.

The implementation begins with one tree node for every distinct sample value. It repeatedly combines the two least frequent nodes until one root remains. Walking left adds a zero and walking right adds a one. Since symbols only appear at leaves, no code can be the prefix of another code.

The application packs those bits into bytes, decodes them through the tree, and compares the result with the original samples. Huffman coding is lossless, so one mismatch means the operation failed.

An interview discussion should mention that raw PCM samples often have many distinct values. Predictive coding or delta values can improve the symbol distribution before Huffman coding.

## Dithering

Ordered dithering uses a small threshold matrix. Repeating the matrix creates a stable pattern that makes a black and white image appear to contain intermediate tones.

Floyd Steinberg dithering makes a hard black or white choice at each pixel. It distributes the resulting error across nearby pixels that have not been processed yet. The pattern is less regular and often preserves detail more naturally.

## YUV brightness and colour

RGB stores red, green, and blue directly. YUV separates luma, which approximates perceived brightness, from two chroma components. The image studio uses a full-range BT.601 conversion.

The brightness control adds an offset to Y while leaving U and V unchanged. The saturation control keeps Y unchanged and scales U and V. A scale of zero removes colour, while values above one exaggerate colour differences. Values are clipped when the transformed RGB result falls outside the displayable range.

This is useful because a direct RGB brightness edit can change colour balance. Working in YUV makes the intended separation explicit, even though rounding and clipping can introduce small differences after conversion back to RGB.

## Monochrome colourization

The colourization tool first measures luminance. Black maps to a selected shadow colour, white maps to a selected highlight colour, and intermediate tones follow a smooth curve between them. A strength control blends the mapped result with the grayscale source.

This technique is also called duotone mapping or pseudo-colour. It creates an expressive colour treatment, but it cannot recover colours that were discarded from the original image. True historical photo colourization needs semantic information or a trained model.

## DCT compression

The discrete cosine transform expresses an eight by eight image block as spatial frequencies. The upper left coefficient represents the average intensity. Values farther from that corner represent faster changes.

Quantization is the lossy step. Dividing by larger values removes fine detail by turning small high frequency coefficients into zero. The quality setting scales the quantization table. An inverse DCT reconstructs the visible image.

PSNR compares the reconstructed image with the original. A higher value usually means less numerical distortion, although it does not perfectly describe human perception.

## Fourier spectrum

The Fourier transform represents the whole image as frequencies. The implementation transforms every row and then every column. Moving the low frequencies to the centre makes the result easier to inspect.

Brightness in the spectrum uses a logarithmic scale because a few low frequency values are usually much larger than everything else.

## What to be ready to explain

- Why Huffman codes are prefix free
- Why real WAV parsers scan chunks
- Why DCT uses small blocks
- Where DCT becomes lossy
- How error diffusion differs from ordered dithering
- Why separating luma and chroma makes brightness edits easier to reason about
- Why pseudo-colour is not the same as recovering original colour
- Why an FFT prefers power of two dimensions
- What PSNR can and cannot tell us
