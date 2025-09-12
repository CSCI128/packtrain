import { Client } from "@stomp/stompjs";
import { useEffect, useRef } from "react";
import SockJS from "sockjs-client";

interface ConnectionOptions {
  authToken: string;
  onConnect: (client: Client) => void;
}

export function useWebSocketClient({
  authToken,
  onConnect,
}: ConnectionOptions) {
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!authToken) return;

    const socket = new SockJS(
      window.__ENV__?.VITE_API_URL
        ? window.__ENV__.VITE_API_URL + "ws"
        : "https://localhost.dev/api/ws"
    );
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: { Authorization: `Bearer ${authToken}` },
      reconnectDelay: 5000,
      onConnect: () => onConnect?.(client),
    });

    clientRef.current = client;
    client.activate();
  }, [authToken, onConnect]);

  return clientRef;
}
