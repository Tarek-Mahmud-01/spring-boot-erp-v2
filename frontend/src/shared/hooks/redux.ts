import { useDispatch, useSelector } from "react-redux";
import type { AppDispatch, RootState } from "@/app/store";

/** Typed Redux hooks (ARCHITECTURE.md §3 shared/hooks). Use everywhere. */
export const useAppDispatch = useDispatch.withTypes<AppDispatch>();
export const useAppSelector = useSelector.withTypes<RootState>();
