import tensorflow as tf

EXPORT_DIR="./"
# graph_def = tf.Graph()
# with graph_def.as_default():
a = tf.placeholder(tf.float32, name='input')
b = tf.constant(3.0, dtype=tf.float32, name='b')
# b = tf.get_variable("b", initializer=tf.constant(3.0))
sess = tf.Session()

sess.run(tf.global_variables_initializer())
adder_node = tf.add(a,b,name='output')

print(tf.shape(adder_node))
print(sess.run(adder_node, {a : [1,2,3,111]}))

# saver = tf.train.Saver()
# save_path = saver.save(sess, "model_path")
# tf.train.write_graph(sess.graph, '.', 'train.pb', as_text=False)

tf.train.write_graph(sess.graph, EXPORT_DIR, '../../graph_basic.pb', as_text=False)
