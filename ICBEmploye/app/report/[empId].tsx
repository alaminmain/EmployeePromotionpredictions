import { useEffect, useMemo, useState } from "react";
import {
  ActivityIndicator,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { useLocalSearchParams } from "expo-router";
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

const DEFAULT_DATE = "2036-06-01";

export default function ReportScreen() {
  if (Platform.OS === "web") {
    return <WebOnlyNotice />;
  }

  return <NativeReportScreen />;
}

function NativeReportScreen() {
  const { empId } = useLocalSearchParams<{ empId: string }>();
  const db = useSQLiteContext();
  const [employee, setEmployee] = useState<EmployeeInfo | null>(null);
  const [predictions, setPredictions] = useState<PredictionRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [targetDate, setTargetDate] = useState(DEFAULT_DATE);

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
        setEmployee(info ?? null);
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [db, empId]);

  useEffect(() => {
    const loadReport = async () => {
      if (!empId) return;
      const rows = await db.getAllAsync<PredictionRow>(
        `SELECT FromGrade, ToGrade, NewDesignation, PredictedDate
         FROM Predictions
         WHERE EmpId = ? AND PredictedDate <= ?
         ORDER BY PredictedDate`,
        [empId, targetDate]
      );
      setPredictions(rows);
    };

    loadReport();
  }, [db, empId, targetDate]);

  const finalPosition = useMemo(() => {
    if (predictions.length === 0) return null;
    return predictions[predictions.length - 1];
  }, [predictions]);

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" />
        <Text style={styles.muted}>Loading report...</Text>
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
      <Text style={styles.header}>Promotion Report</Text>
      <Text style={styles.meta}>Name: {employee.Name}</Text>
      <Text style={styles.meta}>ID: {employee.EmpId}</Text>
      <Text style={styles.meta}>Current Grade: {employee.CurrentGrade}</Text>

      <Text style={styles.label}>Target Date (YYYY-MM-DD)</Text>
      <TextInput
        style={styles.input}
        value={targetDate}
        onChangeText={setTargetDate}
        placeholder="YYYY-MM-DD"
        placeholderTextColor="#7c7c7c"
      />

      <View style={styles.summary}>
        <Text style={styles.summaryText}>
          Total Promotions: {predictions.length}
        </Text>
        {finalPosition && (
          <Text style={styles.summaryText}>
            Final Position: {finalPosition.NewDesignation}
          </Text>
        )}
      </View>

      <Text style={styles.sectionTitle}>Promotion Path</Text>

      {predictions.length === 0 && (
        <Text style={styles.muted}>No promotions by this date.</Text>
      )}

      {predictions.map((pred, index) => (
        <View key={`${pred.PredictedDate}-${index}`} style={styles.row}>
          <Text style={styles.rowDate}>{pred.PredictedDate}</Text>
          <Text style={styles.rowGrades}>
            {pred.FromGrade} → {pred.ToGrade}
          </Text>
          <Text style={styles.rowRole}>{pred.NewDesignation}</Text>
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
  header: {
    fontSize: 22,
    fontWeight: "700",
    color: "#111827",
  },
  meta: {
    color: "#6b7280",
  },
  label: {
    marginTop: 8,
    fontWeight: "600",
    color: "#374151",
  },
  input: {
    backgroundColor: "#ffffff",
    borderColor: "#e2e8f0",
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    color: "#111827",
  },
  summary: {
    marginTop: 4,
    backgroundColor: "#ffffff",
    borderRadius: 12,
    padding: 12,
    borderWidth: 1,
    borderColor: "#e5e7eb",
    gap: 4,
  },
  summaryText: {
    fontWeight: "600",
    color: "#111827",
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: "600",
    color: "#111827",
  },
  row: {
    backgroundColor: "#ffffff",
    borderRadius: 12,
    padding: 12,
    borderWidth: 1,
    borderColor: "#e5e7eb",
    gap: 4,
  },
  rowDate: {
    fontWeight: "600",
    color: "#2563eb",
  },
  rowGrades: {
    fontSize: 16,
    fontWeight: "600",
    color: "#111827",
  },
  rowRole: {
    color: "#4b5563",
  },
  muted: {
    color: "#6b7280",
  },
});
