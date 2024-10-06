package ait.mediation;


import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BlkQueueImpl<T> implements BlkQueue<T> {
    private LinkedList<T> messages;
    private int messagesLimit;
    private Lock mutexSender = new ReentrantLock();
    private Lock mutexReceiver = new ReentrantLock();
    private Condition senderWaitCondition = mutexSender.newCondition();
    private Condition receiverWaitCondition = mutexReceiver.newCondition();

    public BlkQueueImpl(int maxSize) {
        messages = new LinkedList<>();
        messagesLimit = maxSize;
    }

    @Override
    public void push(T message) {
        mutexSender.lock();
        try {
            while(messages.size() >= messagesLimit) {
                try {
                    senderWaitCondition.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mutexReceiver.lock();
            try {
                messages.push(message);
                receiverWaitCondition.signal();
            } finally {
                mutexReceiver.unlock();
            }
        } finally {
            mutexSender.unlock();
        }
    }

    @Override
    public T pop() {
        mutexReceiver.lock();
        try {
            while(messages.isEmpty()) {
                try {
                    receiverWaitCondition.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mutexSender.lock();
            try {
                T res = messages.pop();
                senderWaitCondition.signal();
                return res;
            } finally {
                mutexSender.unlock();
            }
        } finally {
            mutexReceiver.unlock();
        }
    }
}
