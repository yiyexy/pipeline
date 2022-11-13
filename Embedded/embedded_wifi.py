import cv2
import copy
import numpy as np
import onnxruntime
import multiprocessing
import time
import json
import socket
import argparse

def parse_args():
    parser = argparse.ArgumentParser(
        description='Lane detection model')
    parser.add_argument('onnx_path',help='The path of onnx file')
    parser.add_argument('video_path',help='The path of vedio.')
    parser.add_argument('--out',default=None)
    parser.add_argument('--is_show',default=False,help='Whether to show the predion panel.')
    args = parser.parse_args()
    return args

def preprocess(img):
    ori_img = copy.deepcopy(img)
    mean=np.array([123.675, 116.28, 103.53])
    std=np.array([58.395, 57.12, 57.375])
    to_rgb=True
    w,h,c = img.shape
    img = cv2.resize(img,(int(w/2.5),int(h/2.5)))
    img = img.copy().astype(np.float32)
    mean = np.float64(mean.reshape(1, -1))
    stdinv = 1 / np.float64(std.reshape(1, -1))
    if to_rgb:
        cv2.cvtColor(img, cv2.COLOR_BGR2RGB, img)  # inplace
    cv2.subtract(img, mean, img)  # inplace
    cv2.multiply(img, stdinv, img)  # inplace
    if img.dtype == np.uint8:
            img = img.astype(np.float32)
    if len(img.shape) < 3:
        img = np.expand_dims(img, -1)
    img = np.ascontiguousarray(img.transpose(2, 1, 0))  #(h,w,)->(c,h,w)
    img = np.expand_dims(img, axis=0)
    return ori_img,img

results = []
HOST_IP = "192.168.0.134"  # Server Ip
HOST_PORT = 7655  # Port
print("Starting socket: TCP...")
socket_tcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)  
print("TCP server listen @ %s:%d!" % (HOST_IP, HOST_PORT))
host_addr = (HOST_IP, HOST_PORT)
socket_tcp.bind(host_addr)  
socket_tcp.listen(1)  
print('waiting for connection...')
socket_con, (client_ip, client_port) = socket_tcp.accept()  
print("Connection accepted from %s." % client_ip)
times = []
def rec_fun(p_conn):
    all_rec = 0
    s, r = p_conn
 
    while True:
        all_rec += 1
        print('\nreceiving.....', all_rec)
 
        try:
            all_time = time.time()
            outputs =  r.recv()[0]  # If there is no data in pipe, it will be stuck here with no notification!!!
            data = json.dumps(outputs)
            mob_time = time.time()
            send_time = time.time()
            socket_con.send(bytes(data.encode('utf-8')))
            back_time = time.time()
            recive_data = socket_con.recv(1024)  # Receive data
            all_mob = time.time() - mob_time
            print('Communication time: ',all_mob)
            results.append(recive_data)
            times.append(time.time()-all_time)
        except Exception as e:  # this except has no use when stucked above!
            print(e)
            print('can not recv')

def sed_fun(p_conn):
    args = parse_args()
    onnx_path = args.onnx_file
    s,r = p_conn
    video_path = args.video_path
    session = onnxruntime.InferenceSession(onnx_path)

    capture = cv2.VideoCapture(video_path)
    if capture.isOpened():
        while True:
            local_time = time.time()
            ret,img=capture.read() 
            s.send([img.tolist()])
            ori_img,img = preprocess(img)
            outputs = session.run([], {"img": img })
            outputs = np.array(outputs).tolist()
            
            if not ret:
                break 

if __name__ == '__main__':
    sed,rec = multiprocessing.Pipe()
    sed_proc = multiprocessing.Process(target=sed_fun, args=((sed, rec), ))
    rec_proc = multiprocessing.Process(target=rec_fun, args=((sed, rec), ))
 
    sed_proc.start()
    rec_proc.start()
 
    sed_proc.join()
    rec_proc.join()
