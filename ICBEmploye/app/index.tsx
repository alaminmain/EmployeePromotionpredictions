import { useEffect, useMemo, useState } from "react";
import {
  ActivityIndicator,
  FlatList,
  Platform,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";
import { useRouter } from "expo-router";
import { useSQLiteContext } from "expo-sqlite";
import WebOnlyNotice from "../components/WebOnlyNotice";

type EmployeeRow = {
  EmpId: string;
  Name: string;
  CurrentGrade: string;
};

export default function EmployeeListScreen() {
  if (Platform.OS === "web") {
    return <WebOnlyNotice />;
  }

  return <NativeEmployeeListScreen />;
}

function NativeEmployeeListScreen() {
  const db = useSQLiteContext();
  const router = useRouter();
  const [employees, setEmployees] = useState<EmployeeRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");

  useEffect(() => {
    const load = async () => {
      try {
        const rows = await db.getAllAsync<EmployeeRow>(
          `SELECT EmpId, Name, MIN(FromGrade) AS CurrentGrade
           FROM Predictions
           GROUP BY EmpId, Name
           ORDER BY Name`
        );
        setEmployees(rows);
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [db]);

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return employees;
    return employees.filter(
      (emp) =>
        emp.Name.toLowerCase().includes(term) ||
        emp.EmpId.toLowerCase().includes(term)
    );
  }, [employees, search]);

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" />
        <Text style={styles.muted}>Loading employees...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>ICB Employe</Text>
      <Text style={styles.subtitle}>Search and view promotion paths</Text>

      <TextInput
        placeholder="Search by name or ID"
        value={search}
        onChangeText={setSearch}
        style={styles.search}
        placeholderTextColor="#7c7c7c"
      />

      <FlatList
        data={filtered}
        keyExtractor={(item) => item.EmpId}
        contentContainerStyle={styles.listContent}
        ListEmptyComponent={
          <Text style={styles.muted}>No employees found.</Text>
        }
        renderItem={({ item }) => (
          <TouchableOpacity
            style={styles.card}
            onPress={() => router.push(`/employee/${item.EmpId}`)}
          >
            <View>
              <Text style={styles.cardTitle}>{item.Name}</Text>
              <Text style={styles.cardMeta}>
                ID: {item.EmpId} • Grade: {item.CurrentGrade}
              </Text>
            </View>
            <Text style={styles.cardAction}>View</Text>
          </TouchableOpacity>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
    backgroundColor: "#f8f9fb",
  },
  centered: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    gap: 12,
    padding: 16,
  },
  title: {
    fontSize: 28,
    fontWeight: "700",
    color: "#1f2933",
  },
  subtitle: {
    fontSize: 14,
    color: "#5f6c7b",
    marginTop: 4,
    marginBottom: 16,
  },
  search: {
    backgroundColor: "#ffffff",
    borderColor: "#e2e8f0",
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 10,
    marginBottom: 12,
    fontSize: 16,
    color: "#1f2933",
  },
  listContent: {
    paddingBottom: 24,
    gap: 12,
  },
  card: {
    backgroundColor: "#ffffff",
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: "#e5e7eb",
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    shadowColor: "#0f172a",
    shadowOpacity: 0.08,
    shadowRadius: 12,
    elevation: 2,
  },
  cardTitle: {
    fontSize: 16,
    fontWeight: "600",
    color: "#111827",
  },
  cardMeta: {
    marginTop: 4,
    fontSize: 12,
    color: "#6b7280",
  },
  cardAction: {
    color: "#2563eb",
    fontWeight: "600",
  },
  muted: {
    color: "#6b7280",
  },
});
