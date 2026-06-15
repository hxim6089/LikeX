import SockJS from 'sockjs-client'
import { Stomp } from '@stomp/stompjs'

let stompClient = null

/**
 * 连接 WebSocket 并订阅通知/私信频道
 * @param {number} userId 当前用户ID
 * @param {function} onNotification 通知回调
 * @param {function} onMessage 私信回调
 */
export function connectWebSocket(userId, onNotification, onMessage) {
    const socket = new SockJS('http://localhost:8888/ws')
    stompClient = Stomp.over(socket)

    // 关闭调试日志
    stompClient.debug = () => { }

    stompClient.connect({}, () => {
        console.log('WebSocket connected')

        // 订阅通知频道
        stompClient.subscribe(`/user/${userId}/queue/notifications`, (msg) => {
            const data = JSON.parse(msg.body)
            if (onNotification) onNotification(data)
        })

        // 订阅私信频道
        stompClient.subscribe(`/user/${userId}/queue/messages`, (msg) => {
            const data = JSON.parse(msg.body)
            if (onMessage) onMessage(data)
        })
    }, (error) => {
        console.error('WebSocket connection error:', error)
    })
}

/**
 * 断开 WebSocket 连接
 */
export function disconnect() {
    if (stompClient) {
        stompClient.disconnect()
        stompClient = null
    }
}

/**
 * 检查连接状态
 */
export function isConnected() {
    return stompClient !== null && stompClient.connected
}
