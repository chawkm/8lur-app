import tensorflow as tf
import sys
import os

from model_definitions import autoencoder_model as model
from model_definitions.networks import moussaka as autoencoder_network

import numpy as np
from skimage import color
import scipy.misc
import glob

# Paths
model_save_path = 'trained_models/deblurring_model'
dataset_path = 'data/100labeledLPforvalidation_same_dims_scaled'
logs_directory = 'evaluate_logs'
EXPORT_DIR="./"

# Parameters
image_width = 270
image_height = 90
batch_size = 1


corrupted = tf.placeholder(tf.float32, (1,270, 90, 3), name='corrupted')
deblurred = tf.placeholder(tf.float32, (1,270, 90, 3), name='deblurred')

global_step = tf.Variable(0, trainable=False)

with tf.Session(config=tf.ConfigProto(allow_soft_placement=True)) as sess:
    network = model.initialise(image_width, image_height, autoencoder_network.autoencoder, batch_size, 0.001, global_step, training=False)
    saver = tf.train.Saver()
    saver.restore(sess, model_save_path)
# sess = tf.Session()

    tf.train.write_graph(sess.graph, EXPORT_DIR, '../deblurring_graph.pb', as_text=False)
