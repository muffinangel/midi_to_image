# In[1]:
import tensorflow as tf
from tensorflow.examples.tutorials.mnist import input_data
import numpy as np
import os

from import_data import DataProvider
from data_info import path_to_data

# load data
dp = DataProvider(path_to_data)
dp.load_data()

# In[2]:

X = tf.placeholder(tf.float32, shape=[None, 49152])
Z = tf.placeholder(tf.float32, shape=[None, 100]) # noise

# In[3]:

def generator(z):
    
    with tf.variable_scope("generator", reuse=tf.AUTO_REUSE):
        x = tf.layers.dense(z, 128, activation = tf.nn.relu)
        x = tf.layers.dense(z, 128*3, activation = tf.nn.relu) #added
        x = tf.layers.dense(z, 128*3*4, activation = tf.nn.relu) #added
        x = tf.layers.dense(z, 49152)
        x = tf.nn.sigmoid(x)

    return x


def discriminator(x):
    with tf.variable_scope("discrminator", reuse=tf.AUTO_REUSE):
        x = tf.layers.dense(x, 128, activation = tf.nn.relu)
        #x = tf.layers.dense(x, 64, activation = tf.nn.relu) #added
        x = tf.layers.dense(x, 1)
        x = tf.nn.sigmoid(x)

    return x

def sample_Z(m, n):
    return np.random.uniform(-1., 1., size=[m, n])

# In[4]:

G_sample = generator(Z)
D_real = discriminator(X)
D_fake = discriminator(G_sample)

D_loss = -tf.reduce_mean(tf.log(D_real) + tf.log(1. - D_fake))
G_loss = -tf.reduce_mean(tf.log(D_fake))

disc_vars = [var for var in tf.trainable_variables() if var.name.startswith("disc")]
gen_vars =  [var for var in tf.trainable_variables() if var.name.startswith("gen")]

D_solver = tf.train.AdamOptimizer(learning_rate=0.0001).minimize(D_loss, var_list=disc_vars)
G_solver = tf.train.AdamOptimizer(learning_rate=0.0001).minimize(G_loss, var_list=gen_vars)

# In[5]:

mb_size = 32
Z_dim = 100 # noise

print("sess")
sess = tf.Session()
print("sess created")
sess.run(tf.global_variables_initializer())
[print("sess intialized")]


if not os.path.exists('music_new/'):
    os.makedirs('music_new/')

i = 0
r = int(len(dp.train) // mb_size)
print("Range = " + str(r))

for it in range(r*100):
    
    # Save generated images every 1000 iterations.
    if it % 10 == 0 and it > 990:
        samples = sess.run(G_sample, feed_dict={Z: sample_Z(4, Z_dim)})
        for j, sample in enumerate(samples):
            filename = 'music_new/' + str(i) + "_nr" + str(j) + '.png'
            print(filename)
            dp.save_content(filename = filename, content = sample)
        i += 1


    print("it = " + str(it))    
        
    X_mb, _ = dp.next_batch(mb_size)
    print(str(X_mb))
    print(str(type(X_mb)))

    X_mb.shape = (mb_size, 49152)
    print(str(type(X_mb)))

    _, D_loss_curr = sess.run([D_solver, D_loss], feed_dict={X: X_mb, Z: sample_Z(mb_size, Z_dim)})
    _, G_loss_curr = sess.run([G_solver, G_loss], feed_dict={Z: sample_Z(mb_size, Z_dim)})

    # Print loss
    if it % 10 == 0:
        print('Iter: {}'.format(it))
        print('D loss: {:.4}'. format(D_loss_curr))
        print('G loss: {:.4}'. format(G_loss_curr))