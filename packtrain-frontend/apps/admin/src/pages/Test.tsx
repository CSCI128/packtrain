import { Client } from "@stomp/stompjs";
import { useEffect, useState } from "react";
import SockJS from "sockjs-client";
import { userManager } from "../auth";

export default function TestPage() {
  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState([]);
  const [userData, setUserData] = useState(null);
  const [stompClient, setStompClient] = useState(null);

  // fetch user token
  useEffect(() => {
    const fetchUser = async () => {
      try {
        const u = await userManager.getUser();
        if (!u) throw new Error("Failed to get user");
        setUserData(u);
      } catch (err) {
        console.error(err);
      }
    };
    fetchUser();
  }, []);

  useEffect(() => {
    if (!userData) return;

    const socket = new SockJS("https://localhost.dev/api/ws");
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${userData.access_token}`,
      },
      reconnectDelay: 5000,
      onConnect: () => {
        console.log("Connected to SockJS");
        client.subscribe("/topic/messages", (msg) =>
          console.log("Received:", msg.body)
        );
      },
    });

    client.activate();
    setStompClient(client);

    // return () => client.deactivate();
  }, [userData]);

  const sendMessage = () => {
    if (stompClient && stompClient.connected && message.trim()) {
      stompClient.publish({
        destination: "/topic/messages",
        body: message,
      });
      setMessage("");
    }
  };

  return (
    <div className="p-4">
      <h1>WebSocket Chat</h1>
      <div className="flex gap-2 mb-4">
        <input
          className="border p-1 flex-1"
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder="Type message..."
        />
        <button
          className="px-3 py-1 bg-blue-500 text-white rounded"
          onClick={sendMessage}
        >
          Send
        </button>
      </div>
      <ul className="list-disc pl-5">
        {messages.map((msg, i) => (
          <li key={i}>{msg}</li>
        ))}
      </ul>
    </div>
  );
}
