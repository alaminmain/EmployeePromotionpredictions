import { useEffect, useState } from "react";
import {
  ActivityIndicator,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useSQLiteContext } from "expo-sqlite";
import WebOnlyNotice from "../../components/WebOnlyNotice";

type EmployeeInfo = {
  Name: string;
  EmpId: string;
  CurrentGrade: string;
};

type PredictionRow = {
  FromGrade: string;
  ToGrade: string;
  NewDesignation: string;
  PredictedDate: string;
};

export default function EmployeeDetailScreen() {
  if (Platform.OS === "web") {
    return <WebOnlyNotice />;
  }

  return <NativeEmployeeDetailScreen />;
}

function NativeEmployeeDetailScreen() {
  const { empId } = useLocalSearchParams<{ empId: string }>();
  const router = useRouter();
  const db = useSQLiteContext();
  const [employee, setEmployee] = useState<EmployeeInfo | null>(null);
  const [predictions, setPredictions] = useState<PredictionRow[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      if (!empId) return;
      try {
        const info = await db.getFirstAsync<EmployeeInfo>(
          `SELECT EmpId, Name, MIN(FromGrade) AS CurrentGrade
           FROM Predictions
           WHERE EmpId = ?
           GROUP BY EmpId, Name`,
          [empId]
        );
        const rows = await db.getAllAsync<PredictionRow>(
          `SELECT FromGrade, ToGrade, NewDesignation, PredictedDate
           FROM Predictions
           WHERE EmpId = ?
           ORDER BY PredictedDate`,
          [empId]
        );
        setEmployee(info ?? null);
        setPredictions(rows);
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [db, empId]);

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" />
        <Text style={styles.muted}>Loading details...</Text>
      </View>
    );
  }

  if (!employee) {
    return (
      <View style={styles.centered}>
        <Text style={styles.muted}>Employee not found.</Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.name}>{employee.Name}</Text>
      <Text style={styles.meta}>ID: {employee.EmpId}</Text>
      <Text style={styles.meta}>Current Grade: {employee.CurrentGrade}</Text>

      <TouchableOpacity
        style={styles.reportButton}
        onPress={() => router.push(`/report/${employee.EmpId}`)}
      >
        <Text style={styles.reportButtonText}>Open Report View</Text>
      </TouchableOpacity>

      <Text style={styles.sectionTitle}>Promotion Timeline</Text>

      {predictions.length === 0 && (
        <Text style={styles.muted}>No promotions found.</Text>
      )}

      {predictions.map((pred, index) => (
        <View key={`${pred.PredictedDate}-${index}`} style={styles.timelineCard}>
          <Text style={styles.timelineDate}>{pred.PredictedDate}</Text>
          <Text style={styles.timelineGrades}>
            {pred.FromGrade} → {pred.ToGrade}
          </Text>
          <Text style={styles.timelineRole}>{pred.NewDesignation}</Text>
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#f8f9fb",
  },
  content: {
    padding: 16,
    gap: 12,
  },
  centered: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    gap: 12,
    padding: 16,
  },
  name: {
    fontSize: 24,
    fontWeight: "700",
    color: "#111827",
  },
  meta: {
    fontSize: 14,
    color: "#6b7280",
  },
  reportButton: {
    marginTop: 8,
    alignSelf: "flex-start",
    backgroundColor: "#2563eb",
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: 10,
  },
  reportButtonText: {
    color: "#ffffff",
    fontWeight: "600",
  },
  sectionTitle: {
    marginTop: 12,
    fontSize: 18,
    fontWeight: "600",
    color: "#111827",
  },
  timelineCard: {
    backgroundColor: "#ffffff",
    borderRadius: 14,
    padding: 14,
    borderWidth: 1,
    borderColor: "#e5e7eb",
    gap: 4,
  },
  timelineDate: {
    color: "#2563eb",
    fontWeight: "600",
  },
  timelineGrades: {
    fontSize: 16,
    fontWeight: "600",
    color: "#111827",
  },
  timelineRole: {
    color: "#4b5563",
  },
  muted: {
    color: "#6b7280",
  },
});
