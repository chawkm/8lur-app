import tensorflow as tf

EXPORT_DIR="./"
# graph_def = tf.Graph()
# with graph_def.as_default():
corrupted = tf.placeholder(tf.float32, (1 , 195, 260, 3), name='corrupted')
corrupted = 255 - corrupted
deblurred = tf.reshape(corrupted, (1 , 195, 260, 3), name='deblurred')
print(deblurred.shape)

sess = tf.Session()

sess.run(tf.global_variables_initializer())

# saver = tf.train.Saver()
# save_path = saver.save(sess, "model_path")
# tf.train.write_graph(sess.graph, '.', 'train.pb', as_text=False)

tf.train.write_graph(sess.graph, EXPORT_DIR, '../../graph_image.pb', as_text=False)
