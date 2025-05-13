import sys
import face_recognition
import os
import numpy as np

DATABASE_DIR = "known_faces"  # Chứa các file ảnh tên như John.jpg, Alice.jpg

def load_known_faces():
    known_encodings = []
    known_names = []

    for filename in os.listdir(DATABASE_DIR):
        if filename.endswith(".jpg"):
            image = face_recognition.load_image_file(os.path.join(DATABASE_DIR, filename))
            encodings = face_recognition.face_encodings(image)
            if encodings:
                known_encodings.append(encodings[0])
                known_names.append(os.path.splitext(filename)[0])
    return known_encodings, known_names

def main():
    if len(sys.argv) < 2:
        print("ERROR|0")
        return

    image_path = sys.argv[1]
    unknown_image = face_recognition.load_image_file(image_path)
    unknown_encodings = face_recognition.face_encodings(unknown_image)

    if not unknown_encodings:
        print("UNKNOWN|0")
        return

    known_encodings, known_names = load_known_faces()

    distances = face_recognition.face_distance(known_encodings, unknown_encodings[0])
    best_match_index = np.argmin(distances)
    similarity = (1 - distances[best_match_index]) * 100

    if similarity > 60:  # ngưỡng nhận diện
        print(f"{known_names[best_match_index]}|{similarity:.2f}")
    else:
        print("UNKNOWN|0")

if __name__ == "__main__":
    main()
