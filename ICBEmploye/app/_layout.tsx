import { Stack } from "expo-router";
import { SQLiteProvider } from "expo-sqlite";
import { StatusBar } from "expo-status-bar";
import { Platform } from "react-native";
import "react-native-reanimated";

export default function RootLayout() {
  if (Platform.OS === "web") {
    return (
      <>
        <Stack screenOptions={{ headerTitleAlign: "center" }}>
          <Stack.Screen name="index" options={{ title: "Employees" }} />
          <Stack.Screen name="employee/[empId]" options={{ title: "Details" }} />
          <Stack.Screen name="report/[empId]" options={{ title: "Report" }} />
        </Stack>
        <StatusBar style="auto" />
      </>
    );
  }

  return (
    <SQLiteProvider
      databaseName="Promotion.db"
      assetSource={require("../assets/Promotion.db")}
    >
      <Stack screenOptions={{ headerTitleAlign: "center" }}>
        <Stack.Screen name="index" options={{ title: "Employees" }} />
        <Stack.Screen name="employee/[empId]" options={{ title: "Details" }} />
        <Stack.Screen name="report/[empId]" options={{ title: "Report" }} />
      </Stack>
      <StatusBar style="auto" />
    </SQLiteProvider>
  );
}
