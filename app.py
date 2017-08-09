import cv2
import numpy as np
import sys

image1 = cv2.imread(sys.argv[1])
image2 = cv2.imread(sys.argv[2])

difference = cv2.subtract(image1, image2)

result = not np.any(difference) 

if result is True:
    print ("The images are the same")
else:
    cv2.imwrite("result.jpg", difference)
    print ("the images are different")
