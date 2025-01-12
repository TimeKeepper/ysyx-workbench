import numpy as np
import cv2
from scipy.signal import ricker
import matplotlib.pyplot as plt

def generate_ricker_wavelet(length, peak_index, a):
    """Generate a Ricker wavelet with specified parameters."""
    wavelet = ricker(length, a)
    # Shift the wavelet so that its peak is at the given index.
    wavelet = np.roll(wavelet, peak_index - (length // 2))
    return wavelet

def plot_wavelet_as_image(wavelet, title="Ricker Wavelet"):
    """Plot the wavelet as an image using OpenCV."""
    # Normalize to [0, 255] for display as grayscale image
    normalized_wavelet = cv2.normalize(wavelet, None, 0, 255, cv2.NORM_MINMAX).astype(np.uint8)
    
    # Convert 1D array into a 2D array (image) by repeating it vertically
    wavelet_image = np.tile(normalized_wavelet, (100, 1))

    # Display the image using OpenCV
    cv2.imshow(title, wavelet_image)
    cv2.waitKey(0)
    cv2.destroyAllWindows()

if __name__ == "__main__":
    # Parameters for the Ricker wavelet
    length = 100  # Length of the wavelet
    peak_index = length // 2  # Center of the wavelet
    a = 10  # The scale parameter for the wavelet

    # Generate the Ricker wavelet
    wavelet = generate_ricker_wavelet(length, peak_index, a)

    # Optionally plot using matplotlib for comparison
    plt.plot(wavelet)
    plt.title("Ricker Wavelet")
    plt.show()

    # Plot the wavelet as an image using OpenCV
    plot_wavelet_as_image(wavelet)