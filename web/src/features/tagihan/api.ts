import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import type { PaymentCategory } from "@/features/tarif/konstanta";
import type { AcademicTerm } from "@/features/mahasiswa/api";

export type InstallmentStatus = "UNPAID" | "PARTIAL" | "PAID" | "OVERDUE";
export type PlanStatus = "ACTIVE" | "COMPLETED" | "CANCELLED";

export type Installment = {
  id: number;
  installmentNo: number;
  dueDate: string;
  amount: string;
  amountPaid: string;
  outstanding: string;
  status: InstallmentStatus;
};

export type PaymentPlan = {
  id: number;
  category: PaymentCategory;
  categoryLabel: string;
  academicYear: string;
  term: AcademicTerm;
  semesterNumber: number | null;
  baseAmount: string;
  discountPercent: string;
  totalAmount: string;
  amountPaid: string;
  remaining: string;
  status: PlanStatus;
  installments: Installment[];
};

export type AmountChange = {
  id: number;
  installmentId: number;
  oldAmount: string;
  newAmount: string;
  reason: string;
  adminId: number;
  createdAt: string;
};

const KEY = "plans";

export function useStudentPlans(studentId: number) {
  return useQuery({
    queryKey: [KEY, studentId],
    queryFn: () => apiFetch<PaymentPlan[]>(`/students/${studentId}/plans`),
  });
}

export function useCreatePlan(studentId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: {
      category: PaymentCategory;
      academicYear: string;
      term: AcademicTerm;
    }) =>
      apiFetch<PaymentPlan>(`/students/${studentId}/plans`, {
        method: "POST",
        body,
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [KEY, studentId] }),
  });
}

export function useUpdateInstallmentAmount(studentId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      installmentId,
      amount,
      reason,
    }: {
      installmentId: number;
      amount: number;
      reason: string;
    }) =>
      apiFetch<AmountChange>(`/installments/${installmentId}/amount`, {
        method: "PATCH",
        body: { amount, reason },
      }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: [KEY, studentId] });
      queryClient.invalidateQueries({
        queryKey: ["amount-changes", variables.installmentId],
      });
    },
  });
}

export function useAmountChanges(installmentId: number | null) {
  return useQuery({
    queryKey: ["amount-changes", installmentId],
    queryFn: () =>
      apiFetch<AmountChange[]>(`/installments/${installmentId}/amount-changes`),
    enabled: installmentId !== null,
  });
}
