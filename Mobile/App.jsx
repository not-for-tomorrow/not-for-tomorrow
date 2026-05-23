import { useMemo, useRef, useState } from "react";
import { StatusBar } from "expo-status-bar";
import {
  Platform,
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import EventSource from "react-native-sse";

export default function App() {
  const [prompt, setPrompt] = useState("");
  const [messages, setMessages] = useState([]);
  const [streaming, setStreaming] = useState(false);
  const streamRef = useRef(null);

  const apiBase = useMemo(() => {
    if (Platform.OS === "android") return "http://10.0.2.2:8080";
    return "http://localhost:8080";
  }, []);

  const onSend = () => {
    const text = prompt.trim();
    if (!text || streaming) return;

    setPrompt("");
    setStreaming(true);
    setMessages((prev) => [...prev, { role: "user", content: text }, { role: "assistant", content: "" }]);

    const url = `${apiBase}/api/ai/chat/stream?prompt=${encodeURIComponent(text)}`;
    const es = new EventSource(url);
    streamRef.current = es;

    es.addEventListener("message", (event) => {
      const chunk = event.data || "";
      if (chunk === "[DONE]") {
        es.close();
        streamRef.current = null;
        setStreaming(false);
        return;
      }

      setMessages((prev) => {
        const next = [...prev];
        const last = next[next.length - 1];
        if (last && last.role === "assistant") {
          last.content += chunk;
        }
        return next;
      });
    });

    es.addEventListener("error", () => {
      es.close();
      streamRef.current = null;
      setStreaming(false);
      setMessages((prev) => {
        const next = [...prev];
        const last = next[next.length - 1];
        if (last && last.role === "assistant" && !last.content) {
          last.content = "Streaming failed. Please try again.";
        }
        return next;
      });
    });
  };

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>HuyVerse Mobile AI Chat</Text>
      <Text style={styles.subtitle}>Phase 2 - SSE streaming demo</Text>

      <ScrollView style={styles.chat} contentContainerStyle={styles.chatContent}>
        {messages.length === 0 ? <Text style={styles.empty}>Start with a prompt.</Text> : null}
        {messages.map((m, idx) => (
          <View key={idx} style={styles.message}>
            <Text style={styles.role}>{m.role === "user" ? "You" : "AI"}</Text>
            <Text style={styles.content}>{m.content}</Text>
          </View>
        ))}
      </ScrollView>

      <View style={styles.inputRow}>
        <TextInput
          style={styles.input}
          value={prompt}
          onChangeText={setPrompt}
          placeholder="Type prompt..."
          editable={!streaming}
        />
        <Pressable style={styles.button} onPress={onSend} disabled={streaming}>
          <Text style={styles.buttonText}>{streaming ? "..." : "Send"}</Text>
        </Pressable>
      </View>

      <StatusBar style="auto" />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#f8fafc",
    paddingHorizontal: 12,
    paddingTop: 8,
  },
  title: {
    fontSize: 20,
    fontWeight: "700",
  },
  subtitle: {
    marginTop: 4,
    marginBottom: 10,
    color: "#475569",
  },
  chat: {
    flex: 1,
    borderWidth: 1,
    borderColor: "#dbe1ea",
    borderRadius: 12,
    backgroundColor: "#fff",
  },
  chatContent: {
    padding: 12,
    gap: 10,
  },
  empty: {
    color: "#64748b",
  },
  message: {
    paddingBottom: 4,
  },
  role: {
    fontWeight: "700",
    marginBottom: 2,
  },
  content: {
    color: "#0f172a",
  },
  inputRow: {
    marginTop: 10,
    marginBottom: 8,
    flexDirection: "row",
    gap: 8,
  },
  input: {
    flex: 1,
    borderWidth: 1,
    borderColor: "#cbd5e1",
    borderRadius: 10,
    backgroundColor: "#fff",
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  button: {
    minWidth: 70,
    borderRadius: 10,
    backgroundColor: "#0f172a",
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 12,
  },
  buttonText: {
    color: "#fff",
    fontWeight: "600",
  },
});
