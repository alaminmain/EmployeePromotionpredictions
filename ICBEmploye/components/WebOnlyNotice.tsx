import { StyleSheet, Text, View } from "react-native";

type Props = {
  title?: string;
  message?: string;
};

export default function WebOnlyNotice({
  title = "Mobile App Required",
  message = "This screen uses the local SQLite database and is available on Android/iOS. Open the app with Expo Go or an emulator.",
}: Props) {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>{title}</Text>
      <Text style={styles.message}>{message}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#f8f9fb",
    alignItems: "center",
    justifyContent: "center",
    padding: 20,
    gap: 10,
  },
  title: {
    fontSize: 22,
    fontWeight: "700",
    color: "#111827",
    textAlign: "center",
  },
  message: {
    fontSize: 14,
    lineHeight: 20,
    color: "#4b5563",
    textAlign: "center",
    maxWidth: 460,
  },
});
