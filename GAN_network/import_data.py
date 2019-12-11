import os 
import os.path
import tensorflow as tf
import numpy as np



class DataProvider:

    def __init__(self, path, train_ratio = 0.9):
        self.path = path
        self.data = None
        self.train_ratio = train_ratio
        self.test_ratio = 1.0 - train_ratio
        self.shuffled = False
        self.train = None
        self.test = None
        self.batch_pos = 0

    def load_data(self, size = 49152):
        names = [name for name in os.listdir(self.path)]
        nr_files = len(names)
        
        # nr_files = 40 # mock

        arr = np.empty((0, size), np.float32)

        for i in range(nr_files):
            res = self.load_content(self.path + names[i])
            arr = np.row_stack((arr, res))

        print(arr.shape)
        self.data = arr
        self.train_set()
        return arr

    def train_set(self, shuffle = True):
        if self.train is not None:
            return self.train
        if self.data is None:
            return None
        if shuffle and not self.shuffled:
            np.random.shuffle(self.data)
            self.shuffled = True
        from sklearn.model_selection import train_test_split
        tr, te, y_tr, y_test = train_test_split(self.data, '1'*(len(self.data)), test_size = self.test_ratio, random_state = 0)
        self.train = tr
        self.y_tr = y_tr
        self.y_test = y_test
        self.test = te
        print("DP tr: " + str(len(tr)))
        print("DP te: " + str(len(te)))
        print("RATIO : " + str(self.test_ratio))
        return self.train
    
    def test_set(self, shuffle = True):
        if self.test is not None:
            return self.test
        if self.data is None:
            return None
        self.train_set(self, shuffle)
        return self.test

    def next_batch(self, size):
        print('next batch: ' + str(self.batch_pos))
        if self.train is None:
            self.train_set()
        if self.batch_pos + size > len(self.train):
            i = 0
            self.batch_pos = size
            np.random.shuffle(self.train)
            return (self.train[i:i+size], '1')
            #(self.train[i:-(len(self.train) - self.batch_pos)], '1')
        i = self.batch_pos
        self.batch_pos += size
        return (self.train[i:i+size], '1')
        

    def load_content(self, filepath, size = [128,128], norm = 255.0):
        file_content = tf.read_file(filepath)
        data = tf.image.decode_png(
            file_content,
            channels=3,
            dtype=tf.uint8,
            name=None
        )
        # it doesnt get the proper shape, so its set manually
        data_resized = tf.image.resize_images(data, size)
        # 1, 49152 (128x128x3)
        data_2d = tf.reshape(data_resized, [1, -1])
        data_2d_normed = tf.cast(data_2d, tf.float32) / norm
        with tf.Session() as sess:
            sess.run(file_content)
            sess.run(data)
            sess.run(data_resized)
            sess.run(data_2d)
            return sess.run(data_2d_normed)

    def save_content(self, filename, content, size = [128,128, 3], norm = 255.0):
        with tf.Session() as sess:
            d2d_back_to = sess.run(tf.reshape(content*norm, size))
            dr_casted = sess.run(tf.cast(d2d_back_to, tf.uint16))
            im_enc = sess.run(tf.image.encode_png(dr_casted))
            sess.run(tf.write_file(filename, im_enc))
            print("!!")
    
    def clear(self):
        self.data = None
        self.shuffled = False
        self.train = None
        self.test = None

# load_data(path_to_data)

if __name__ == "__main__":
    from data_info import path_to_data
    dp = DataProvider(path_to_data)
    dp.load_data()
    r = dp.train_set()
    print(r)