let accounts = [];
let categories = [];
const samples = [
  "昨天用微信买菜花了 68.5，备注晚饭食材",
  "今天招商银行卡收到工资 18000",
  "5月20日从招商银行卡转 3000 到微信钱包，备用金",
  "前天支付宝买日用品 126.8",
  "今天现金调账少了 20，现金盘点差额",
];
let sampleIndex = 0;

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));
const money = (value) => `¥${Number(value || 0).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
const accountName = (id) => accounts.find((item) => item.id === id)?.name || "-";
const categoryName = (id) => categories.find((item) => item.id === id)?.name || "-";

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (response.status === 204) return null;
  const data = await response.json();
  if (!response.ok) throw new Error(data.error || "请求失败");
  return data;
}

function toast(message) {
  $("#toast").textContent = message;
  $("#toast").classList.add("show");
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => $("#toast").classList.remove("show"), 1800);
}

async function loadBaseData() {
  accounts = await api("/api/accounts");
  categories = await api("/api/categories");
  const accountOptions = accounts.map((item) => `<option value="${item.id}">${item.name}</option>`).join("");
  $("#accountId").innerHTML = accountOptions;
  $("#targetAccountId").innerHTML = accountOptions;
  renderCategoryOptions();
}

async function renderDashboard() {
  const data = await api(`/api/reports/overview?month=${encodeURIComponent($("#monthInput").value)}`);
  $("#income").textContent = money(data.income);
  $("#expense").textContent = money(data.expense);
  $("#balance").textContent = money(data.balance);
  $("#totalAsset").textContent = money(data.totalAsset);
  const reconcile = await api("/api/reports/reconcile");
  $("#accountCards").innerHTML = reconcile.map((item) => `
    <article class="card">
      <strong>${item.accountName}</strong>
      <span class="muted">初始余额 ${money(item.openingBalance)}</span>
      <strong>${money(item.currentBalance)}</strong>
    </article>
  `).join("");
}

async function renderTransactions() {
  const params = new URLSearchParams();
  if ($("#keyword").value.trim()) params.set("keyword", $("#keyword").value.trim());
  if ($("#filterType").value) params.set("type", $("#filterType").value);
  const items = await api(`/api/transactions?${params}`);
  $("#transactionRows").innerHTML = items.map((item) => `
    <tr>
      <td>${item.date}</td>
      <td>${labelType(item.type)}</td>
      <td class="${amountClass(item.type)}">${formatAmount(item)}</td>
      <td>${item.type === "TRANSFER" ? `${accountName(item.accountId)} → ${accountName(item.targetAccountId)}` : accountName(item.accountId)}</td>
      <td>${item.categoryId ? categoryName(item.categoryId) : "-"}</td>
      <td>${item.note || "-"}</td>
      <td><button data-delete="${item.id}">删除</button></td>
    </tr>
  `).join("");
}

async function renderReconcile() {
  const rows = await api("/api/reports/reconcile");
  $("#reconcileCards").innerHTML = rows.map((item) => `
    <article class="card">
      <strong>${item.accountName}</strong>
      <span>初始：${money(item.openingBalance)}</span>
      <span>收入：${money(item.income)} / 支出：${money(item.expense)}</span>
      <span>转入：${money(item.transferIn)} / 转出：${money(item.transferOut)}</span>
      <span>调账：${money(item.adjust)}</span>
      <strong>当前：${money(item.currentBalance)}</strong>
    </article>
  `).join("");
}

function labelType(type) {
  return { EXPENSE: "支出", INCOME: "收入", TRANSFER: "转账", ADJUST: "调账" }[type] || type;
}

function amountClass(type) {
  return { EXPENSE: "expense", INCOME: "income", TRANSFER: "transfer", ADJUST: "transfer" }[type] || "";
}

function formatAmount(item) {
  if (item.type === "INCOME") return `+${money(item.amount)}`;
  if (item.type === "EXPENSE") return `-${money(item.amount)}`;
  return money(item.amount);
}

function setType(type) {
  $("#type").value = type;
  $$(".segments button").forEach((button) => button.classList.toggle("active", button.dataset.type === type));
  $("#targetWrap").style.display = type === "TRANSFER" ? "" : "none";
  $("#categoryWrap").style.display = type === "EXPENSE" || type === "INCOME" ? "" : "none";
  renderCategoryOptions();
}

function renderCategoryOptions() {
  const type = $("#type").value;
  $("#categoryId").innerHTML = categories
    .filter((item) => item.type === type)
    .map((item) => `<option value="${item.id}">${item.name}</option>`)
    .join("");
}

function fillForm(data) {
  setType(data.type);
  $("#amount").value = data.amount || "";
  $("#date").value = data.date || "2026-05-28";
  $("#accountId").value = data.accountId || "wechat";
  if (data.targetAccountId) $("#targetAccountId").value = data.targetAccountId;
  if (data.categoryId) $("#categoryId").value = data.categoryId;
  $("#note").value = data.note || "";
  $("#aiResult").textContent = `已读取到表单，可二次修改后保存。\n置信度：${Math.round((data.confidence || 0) * 100)}%`;
}

async function refreshAll() {
  await renderDashboard();
  await renderTransactions();
  await renderReconcile();
}

function bindEvents() {
  $$(".sidebar button").forEach((button) => {
    button.addEventListener("click", () => {
      $$(".sidebar button").forEach((item) => item.classList.remove("active"));
      button.classList.add("active");
      $$(".view").forEach((view) => view.classList.toggle("active", view.id === button.dataset.view));
    });
  });
  $$(".segments button").forEach((button) => button.addEventListener("click", () => setType(button.dataset.type)));
  $("#monthInput").addEventListener("change", renderDashboard);
  $("#searchBtn").addEventListener("click", renderTransactions);
  $("#entryForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const payload = {
      type: $("#type").value,
      amount: $("#amount").value,
      date: $("#date").value,
      accountId: $("#accountId").value,
      targetAccountId: $("#type").value === "TRANSFER" ? $("#targetAccountId").value : null,
      categoryId: ["EXPENSE", "INCOME"].includes($("#type").value) ? $("#categoryId").value : null,
      note: $("#note").value,
    };
    await api("/api/transactions", { method: "POST", body: JSON.stringify(payload) });
    toast("账目已保存");
    await refreshAll();
  });
  $("#sampleVoice").addEventListener("click", () => {
    $("#voiceText").value = samples[sampleIndex % samples.length];
    sampleIndex += 1;
  });
  $("#parseVoice").addEventListener("click", async () => {
    const data = await api("/api/ai/parse-voice", { method: "POST", body: JSON.stringify({ voiceText: $("#voiceText").value }) });
    $("#prompt").value = data.prompt;
    $("#modelJson").value = JSON.stringify(data.modelJson, null, 2);
    $("#aiResult").textContent = "模型 JSON 已生成。使用 ChatGPT Free 时，可复制 Prompt 后把返回 JSON 粘贴回来。";
  });
  $("#copyPrompt").addEventListener("click", async () => {
    try {
      await navigator.clipboard.writeText($("#prompt").value);
      toast("Prompt 已复制");
    } catch {
      toast("请手动复制 Prompt");
    }
  });
  $("#applyJson").addEventListener("click", () => {
    fillForm(JSON.parse($("#modelJson").value));
  });
  $("#transactionRows").addEventListener("click", async (event) => {
    const button = event.target.closest("[data-delete]");
    if (!button) return;
    await api(`/api/transactions/${button.dataset.delete}`, { method: "DELETE" });
    toast("账目已软删除");
    await refreshAll();
  });
}

async function init() {
  $("#date").value = "2026-05-28";
  bindEvents();
  await loadBaseData();
  setType("EXPENSE");
  await refreshAll();
}

init().catch((error) => toast(error.message));
