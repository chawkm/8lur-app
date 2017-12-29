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

checkpoint = tf.train.get_checkpoint_state("trained_models")
input_checkpoint = checkpoint.model_checkpoint_path

# We precise the file fullname of our freezed graph
absolute_model_dir = "/".join(input_checkpoint.split('/')[:-1])
output_graph = absolute_model_dir + "/frozen_model.pb"

# We clear devices to allow TensorFlow to control on which device it will load operations
clear_devices = True

# We start a session using a temporary fresh Graph
with tf.Session(graph=tf.Graph()) as sess:
    # We import the meta graph in the current default Graph
    saver = tf.train.import_meta_graph(input_checkpoint + '.meta', clear_devices=clear_devices)

    # We restore the weights
    saver.restore(sess, input_checkpoint)
    # for op in tf.get_default_graph().get_operations():
    #     if "tanh" in op.name:
    #         print(op.name)
    # print(tf.get_default_graph().get_operations())

    # We use a built-in TF helper to export variables to constants
    output_graph_def = tf.graph_util.convert_variables_to_constants(
        sess, # The session is used to retrieve the weights
        tf.get_default_graph().as_graph_def(), # The graph_def is used to retrieve the nodes
        ["Tanh"] # The output node names are used to select the useful nodes
    )

    # # Finally we serialize and dump the output graph to the filesystem
    with tf.gfile.GFile(output_graph, "wb") as f:
        f.write(output_graph_def.SerializeToString())
    print("%d ops in the final graph." % len(output_graph_def.node))


# corrupted = tf.placeholder(tf.float32, (1,270, 90, 3), name='corrupted')
# deblurred = tf.placeholder(tf.float32, (1,270, 90, 3), name='deblurred')
#
# global_step = tf.Variable(0, trainable=False)
#
# with tf.Session(config=tf.ConfigProto(allow_soft_placement=True)) as sess:
#     network = model.initialise(image_width, image_height, autoencoder_network.autoencoder, batch_size, 0.001, global_step, training=False)
#     saver = tf.train.Saver()
#     saver.restore(sess, model_save_path)
# # sess = tf.Session()
#
#     output_graph_def = tf.graph_util.convert_variables_to_constants(
#         sess, # The session is used to retrieve the weights
#         tf.get_default_graph().as_graph_def(), # The graph_def is used to retrieve the nodes
#         ["deblurred"]# The output node names are used to select the usefull nodes
#     )
#
#     tf.train.write_graph(output_graph_def, EXPORT_DIR, '../deblurring_graph.pb', as_text=False)




# run the following to optimize the graph!!
# python -m tensorflow.python.tools.optimize_for_inference --input trained_models/frozen_model.pb --output graph_optimized.pb --input_names=corrupted --output_names=Tanh