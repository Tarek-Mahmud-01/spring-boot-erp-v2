import { configureStore } from "@reduxjs/toolkit";
import { masterDataApi } from "@/shared/services/masterDataApi";
import accessUsersReducer from "@/modules/access/slice/usersSlice";
import currencyReducer from "@/modules/settings/currency/slice/currencySlice";
import taxcodeReducer from "@/modules/settings/taxcode/slice/taxcodeSlice";
import locationReducer from "@/modules/settings/location/slice/locationSlice";
import numberingReducer from "@/modules/settings/numbering/slice/numberingSlice";
import companyReducer from "@/modules/settings/company/slice/companySlice";
import productReducer from "@/modules/product/catalog/slice/productSlice";
import supplierReducer from "@/modules/procurement/suppliers/slice/supplierSlice";
import purchaseOrderReducer from "@/modules/procurement/orders/slice/purchaseOrderSlice";
import requisitionReducer from "@/modules/procurement/requisitions/slice/requisitionSlice";
import receiptReducer from "@/modules/procurement/receipts/slice/receiptSlice";
import billReducer from "@/modules/procurement/bills/slice/billSlice";
import stockReducer from "@/modules/inventory/stock/slice/stockSlice";
import adjustmentReducer from "@/modules/inventory/adjustments/slice/adjustmentSlice";
import transferReducer from "@/modules/inventory/transfers/slice/transferSlice";
import authReducer from "./authSlice";
import themeReducer from "./themeSlice";
import toastReducer from "./toastSlice";

/**
 * Root store (ARCHITECTURE.md §3). Global slices (auth, theme, …) + the
 * master-data RTK Query reducer. Feature modules inject their own slices via
 * their module registration — kept out of here so modules stay self-contained.
 */
export const store = configureStore({
  reducer: {
    auth: authReducer,
    theme: themeReducer,
    toast: toastReducer,
    accessUsers: accessUsersReducer,
    currency: currencyReducer,
    taxcode: taxcodeReducer,
    location: locationReducer,
    numbering: numberingReducer,
    company: companyReducer,
    product: productReducer,
    supplier: supplierReducer,
    purchaseOrder: purchaseOrderReducer,
    requisition: requisitionReducer,
    receipt: receiptReducer,
    bill: billReducer,
    stock: stockReducer,
    adjustment: adjustmentReducer,
    transfer: transferReducer,
    [masterDataApi.reducerPath]: masterDataApi.reducer,
  },
  middleware: (getDefault) => getDefault().concat(masterDataApi.middleware),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
