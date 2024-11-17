import cv2
import numpy as np

image_array = cv2.imread('subui.png')

gray = cv2.cvtColor(image_array, cv2.COLOR_BGR2GRAY)
_, thresh = cv2.threshold(gray, 1, 255, cv2.THRESH_BINARY)
contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
x, y, w, h = cv2.boundingRect(contours[0])
cropped_image = image_array[y:y+h, x:x+w]

# Resize the cropped image to 200x120
image_array = cv2.resize(cropped_image, (200, 120))

[height, width, channels] = image_array.shape

red = image_array[:, :, 2]
green = image_array[:, :, 1]
blue = image_array[:, :, 0]

r = red.flatten().astype(np.uint32)
g = green.flatten().astype(np.uint32)
b = blue.flatten().astype(np.uint32)

rgb = np.zeros(height * width, dtype=np.uint32)

for i in range(height * width):
    rgb[i] = ((r[i] >> 4) << 8) | ((g[i] >> 4) << 4) | (b[i] >> 4)

with open('subui.coe', 'w') as f:
    f.write("memory_initialization_radix=16;\n")
    f.write("memory_initialization_vector=\n")
    for i in range(len(rgb)):
        # 将每个 RGB 值以 16 进制格式写入文件
        f.write(f"{hex(rgb[i])[2:].zfill(3)},\n")  # zfill(3) 保证 3 位输出
    f.write(";")

cv2.imshow('image', image_array)
cv2.waitKey(0)
cv2.destroyAllWindows()
