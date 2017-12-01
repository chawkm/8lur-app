import tensorflow as tf
# import cnn_denoiser.input_data as input_data
import glob
import os

# Parameters
channels = 3

dataset_path = 'data/pretraining'
image_width = 270
image_height = 90
batch_size = 64

# image_data = input_data.load_images(dataset_path, image_width, image_height)

# pretrain_steps_first_layer = 1000
# pretrain_steps = 100

training_dropout = 0.5

start_learning_rate = 0.001
steps_before_decay = 1000
decay_rate = 0.995

global_step = tf.Variable(0, trainable=False)
alpha = tf.train.exponential_decay(start_learning_rate,
                                   global_step,
                                   steps_before_decay,
                                   decay_rate,
                                   staircase=True)

def summary_layer(net, name):
    pass
    # tf.summary.image(name, tf.expand_dims(tf.transpose(net, [3, 0, 1, 2])[0], 3), max_outputs=1)

def conv_layer_dropout(net, layer, out_channels, filter_dims, strides, padding, name, dropout=0.5, act_f = tf.nn.relu, pre=False):
    net = tf.layers.dropout(net, dropout)
    return conv_layer(net, layer, out_channels, filter_dims, strides, padding, name, act_f, pre=pre)

def conv_layer(net, layer, out_channels, filter_dims, strides, padding, name, act_f = tf.nn.relu, pre=False):
    net = layer(net, out_channels, filter_dims, strides=strides, padding=padding)
    net = act_f(net)
    if not pre:
        summary_layer(net, name)
    return net

def pre_train_conv_layer(inputs, layer, out_channels, filt, strides, name, act_f = tf.nn.relu, dropout=0.5):
    forward = layer
    backward = tf.layers.conv2d_transpose if layer is tf.layers.conv2d else tf.layers.conv2d

    name_scope = name + '_pretrain_scope'
    with tf.variable_scope(name_scope) as vs:
        net = conv_layer_dropout(inputs, forward, out_channels, filt, strides, 'SAME', name, dropout=dropout)
        out = conv_layer_dropout(net, backward, int(inputs.shape[-1]), filt, strides, 'SAME', name + '_pretrain', pre=True)

        trainable_variables = tf.get_collection(tf.GraphKeys.GLOBAL_VARIABLES, scope=vs.name)
        var_list = [v for v in trainable_variables if name in v.name]
        if len(var_list) != 4:
            raise Exception("No two unique output layers to pretrain")

        cost = tf.reduce_mean(tf.square(out - inputs))
        step = tf.train.GradientDescentOptimizer(alpha).minimize(cost, var_list=var_list, global_step=global_step)

        return net, step, cost

files = glob.glob('./pretrain/*')
for f in files:
    os.remove(f)

# writer = tf.summary.FileWriter("./pretrain", graph=tf.get_default_graph())
# def pretrain(epochs, step, loss, placeholder, name, training):
#     if not training:
#         return
#
#     sess = tf.Session(config=tf.ConfigProto(allow_soft_placement=True))
#     sess.run(tf.global_variables_initializer())
#     summary_op = tf.summary.merge_all()
#
#     for i in range(epochs):
#         input_, blurred = image_data.next_batch(batch_size)
#         _, cost, summary = sess.run([step, loss, summary_op], feed_dict={placeholder: blurred})
#         writer.add_summary(summary, i)
#         print(i, "Pretrain " + name, cost)


def autoencoder(original, inputs, training):
    dropout = training_dropout if training else 0.0

    # Encoder
    net, step, loss = pre_train_conv_layer(inputs, tf.layers.conv2d, 512, [5, 5], (3, 3), 'conv1', dropout=0.0)
    print(net.shape)
    # pretrain(pretrain_steps_first_layer, step, loss, original, 'conv1', training)

    net, step, loss = pre_train_conv_layer(net, tf.layers.conv2d, 256, [5, 5], (3, 3), 'conv2', dropout=dropout)
    print(net.shape)
    # pretrain(pretrain_steps, step, loss, original, 'conv2', training)

    net, step, loss = pre_train_conv_layer(net, tf.layers.conv2d, 128, [5, 5], (1, 1), 'conv3', dropout=dropout)
    print(net.shape)
    # pretrain(pretrain_steps, step, loss, original, 'conv3', training)

    # Decoder
    net, step, loss  = pre_train_conv_layer(net, tf.layers.conv2d_transpose, 128, [5, 5], (1, 1), 'deconv1', dropout=dropout)
    print(net.shape)
    # pretrain(pretrain_steps, step, loss, original, 'deconv1', training)

    net, step, loss  = pre_train_conv_layer(net, tf.layers.conv2d_transpose, 256, [5, 5], (3, 3), 'deconv2', dropout=dropout)
    print(net.shape)
    # pretrain(pretrain_steps, step, loss, original, 'deconv2', training)

    net, step, loss  = pre_train_conv_layer(net, tf.layers.conv2d_transpose, channels, [5, 5], (3, 3), 'deconv3', dropout=dropout)
    print(net.shape)
    # pretrain(pretrain_steps, step, loss, original, 'deconv3', training)


    # Final tanh activation
    net = tf.nn.tanh(net, name = 'deblurred2')

    return net
