/**
 * Employee Database - Employee Management System
 * Frontend Application Logic (Vanilla JS + Bootstrap 5)
 */

// Base API URL configuration:
// Automatically uses current host:port if served by Spring Boot, or points to port 8081 if served statically
const API_BASE_URL = (window.location.port === '8081' || window.location.port === '8080')
    ? `${window.location.origin}/api/employees`
    : 'http://localhost:8081/api/employees';

// Global state
let currentEmployees = [];
let currentRole = localStorage.getItem('ems_role') || 'ADMIN'; // 'ADMIN' or 'HR'
let activeDeleteId = null;

// Initialize on DOM Load
document.addEventListener('DOMContentLoaded', () => {
    initRole();
    initEventListeners();

    // Check if on dashboard / list page or add-edit page
    if (document.getElementById('employeeTableBody')) {
        loadEmployees();
        loadStats();
    }

    if (document.getElementById('employeeForm')) {
        initFormPage();
    }
});

/**
 * Initializes and syncs current user role
 */
function initRole() {
    const roleSelect = document.getElementById('roleSelect');
    if (roleSelect) {
        roleSelect.value = currentRole;
        roleSelect.addEventListener('change', (e) => {
            currentRole = e.target.value;
            localStorage.setItem('ems_role', currentRole);
            updateRoleUI();
            if (document.getElementById('employeeTableBody')) {
                renderEmployeeTable(currentEmployees);
            }
            showToast(`Role switched to ${currentRole}`, 'info');
        });
    }
    updateRoleUI();
}

function updateRoleUI() {
    const roleBadge = document.getElementById('currentRoleDisplay');
    if (roleBadge) {
        roleBadge.textContent = currentRole;
    }

    // Hide or disable Add Employee button for roles without add permission if ever needed
    const addBtns = document.querySelectorAll('.admin-hr-only');
    addBtns.forEach(btn => {
        btn.style.display = (currentRole === 'ADMIN' || currentRole === 'HR') ? 'inline-flex' : 'none';
    });
}

/**
 * Event Listeners for Search & Filter
 */
function initEventListeners() {
    const searchInput = document.getElementById('searchInput');
    const searchBtn = document.getElementById('searchBtn');
    const deptFilter = document.getElementById('departmentFilter');
    const statusFilter = document.getElementById('statusFilter');
    const resetBtn = document.getElementById('resetFiltersBtn');

    if (searchInput) {
        // Instant search with debounce
        let debounceTimer;
        searchInput.addEventListener('input', () => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                applyFilters();
            }, 300);
        });
    }

    if (searchBtn) {
        searchBtn.addEventListener('click', (e) => {
            e.preventDefault();
            applyFilters();
        });
    }

    if (deptFilter) {
        deptFilter.addEventListener('change', applyFilters);
    }

    if (statusFilter) {
        statusFilter.addEventListener('change', applyFilters);
    }

    if (resetBtn) {
        resetBtn.addEventListener('click', () => {
            if (searchInput) searchInput.value = '';
            if (deptFilter) deptFilter.value = 'ALL';
            if (statusFilter) statusFilter.value = 'ALL';
            loadEmployees();
        });
    }

    // Delete modal confirmation button
    const confirmDeleteBtn = document.getElementById('confirmDeleteBtn');
    if (confirmDeleteBtn) {
        confirmDeleteBtn.addEventListener('click', handleConfirmDelete);
    }
}

/**
 * Load dashboard statistics
 */
async function loadStats() {
    try {
        const response = await fetch(`${API_BASE_URL}/stats`);
        if (!response.ok) throw new Error('Failed to fetch stats');
        const stats = await response.json();

        const totalEl = document.getElementById('statTotal');
        const activeEl = document.getElementById('statActive');
        const inactiveEl = document.getElementById('statInactive');

        if (totalEl) totalEl.textContent = stats.total || 0;
        if (activeEl) activeEl.textContent = stats.active || 0;
        if (inactiveEl) inactiveEl.textContent = stats.inactive || 0;
    } catch (err) {
        console.warn('Could not load server stats, falling back to local calculation:', err);
    }
}

/**
 * Fetch all employees from backend API
 */
async function loadEmployees() {
    showLoading(true);
    try {
        const response = await fetch(API_BASE_URL);
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const employees = await response.json();
        currentEmployees = employees;
        renderEmployeeTable(employees);
        updateLocalStats(employees);
    } catch (error) {
        console.error('Error loading employees:', error);
        showToast('Unable to connect to Spring Boot backend API. Ensure server is running on port 8081.', 'danger');
        renderEmptyState('Failed to load employee records from server.');
    } finally {
        showLoading(false);
    }
}

/**
 * Filter and Search logic
 */
async function applyFilters() {
    const searchVal = document.getElementById('searchInput')?.value.trim() || '';
    const deptVal = document.getElementById('departmentFilter')?.value || 'ALL';
    const statusVal = document.getElementById('statusFilter')?.value || 'ALL';

    showLoading(true);
    try {
        let url = API_BASE_URL;

        // If only search term is used
        if (searchVal && (deptVal === 'ALL' || !deptVal) && (statusVal === 'ALL' || !statusVal)) {
            url = `${API_BASE_URL}/search?name=${encodeURIComponent(searchVal)}`;
        } else {
            // Use combined query parameters supported by backend GET /api/employees
            const params = new URLSearchParams();
            if (deptVal && deptVal !== 'ALL') params.append('department', deptVal);
            if (statusVal && statusVal !== 'ALL') params.append('status', statusVal);
            url = `${API_BASE_URL}?${params.toString()}`;
        }

        const res = await fetch(url);
        if (!res.ok) throw new Error('Failed to filter employees');
        let data = await res.json();

        // If search term was provided along with department/status filters, filter in-memory
        if (searchVal && (deptVal !== 'ALL' || statusVal !== 'ALL')) {
            const query = searchVal.toLowerCase();
            data = data.filter(e =>
                (e.firstName && e.firstName.toLowerCase().includes(query)) ||
                (e.lastName && e.lastName.toLowerCase().includes(query)) ||
                (e.email && e.email.toLowerCase().includes(query)) ||
                (e.designation && e.designation.toLowerCase().includes(query))
            );
        }

        currentEmployees = data;
        renderEmployeeTable(data);
    } catch (err) {
        console.error('Filter error:', err);
        showToast('Error applying search/filters', 'danger');
    } finally {
        showLoading(false);
    }
}

/**
 * Render Employee Table
 */
function renderEmployeeTable(employees) {
    const tbody = document.getElementById('employeeTableBody');
    const countDisplay = document.getElementById('tableCountDisplay');
    if (!tbody) return;

    if (countDisplay) {
        countDisplay.textContent = `Showing ${employees.length} employee${employees.length === 1 ? '' : 's'}`;
    }

    if (!employees || employees.length === 0) {
        renderEmptyState('No employee records found matching your criteria.');
        return;
    }

    tbody.innerHTML = employees.map(emp => {
        const initials = `${emp.firstName ? emp.firstName[0] : ''}${emp.lastName ? emp.lastName[0] : ''}`.toUpperCase();
        const statusClass = emp.status?.toLowerCase() === 'active' ? 'badge-status-active' : 'badge-status-inactive';
        const formattedSalary = emp.salary ? `₹${parseFloat(emp.salary).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` : 'N/A';
        const formattedDate = emp.joiningDate ? new Date(emp.joiningDate).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' }) : 'N/A';

        // Delete button is restricted based on role (Section 15: ADMIN can delete; HR cannot delete)
        const deleteButtonHtml = (currentRole === 'ADMIN')
            ? `<button class="btn btn-sm btn-outline-danger btn-action" onclick="openDeleteModal(${emp.id}, '${escapeQuotes(emp.fullName || emp.firstName)}')" title="Delete Employee">
                 <i class="bi bi-trash"></i>
               </button>`
            : `<button class="btn btn-sm btn-outline-secondary btn-action" disabled title="Delete restricted to ADMIN">
                 <i class="bi bi-lock-fill"></i>
               </button>`;

        return `
            <tr>
                <td class="text-muted fw-semibold">#${emp.id}</td>
                <td>
                    <div class="d-flex align-items-center">
                        <div class="employee-avatar">${initials}</div>
                        <div>
                            <div class="fw-bold text-dark">${escapeHtml(emp.fullName || `${emp.firstName} ${emp.lastName}`)}</div>
                            <div class="text-muted small">${escapeHtml(emp.email)}</div>
                        </div>
                    </div>
                </td>
                <td><span class="badge-department">${escapeHtml(emp.department || 'N/A')}</span></td>
                <td>
                    <div class="fw-medium">${escapeHtml(emp.designation || 'N/A')}</div>
                    <div class="text-muted small">${formattedSalary}</div>
                </td>
                <td><span class="badge badge-status ${statusClass}">${escapeHtml(emp.status || 'Active')}</span></td>
                <td class="text-muted small">${formattedDate}</td>
                <td>
                    <div class="d-flex gap-1">
                        <button class="btn btn-sm btn-outline-info btn-action" onclick="viewEmployeeDetails(${emp.id})" title="View Profile">
                            <i class="bi bi-eye"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-primary btn-action" onclick="openEditEmployee(${emp.id})" title="Edit Employee">
                            <i class="bi bi-pencil"></i>
                        </button>
                        ${deleteButtonHtml}
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function renderEmptyState(message) {
    const tbody = document.getElementById('employeeTableBody');
    if (!tbody) return;
    tbody.innerHTML = `
        <tr>
            <td colspan="7">
                <div class="empty-state">
                    <div class="empty-state-icon"><i class="bi bi-people"></i></div>
                    <h5 class="text-secondary mb-1">No Records Found</h5>
                    <p class="text-muted small mb-3">${message}</p>
                    <button class="btn btn-sm btn-outline-primary" onclick="resetAllFilters()">Reset Filters</button>
                </div>
            </td>
        </tr>
    `;
}

function resetAllFilters() {
    const searchInput = document.getElementById('searchInput');
    const deptFilter = document.getElementById('departmentFilter');
    const statusFilter = document.getElementById('statusFilter');
    if (searchInput) searchInput.value = '';
    if (deptFilter) deptFilter.value = 'ALL';
    if (statusFilter) statusFilter.value = 'ALL';
    loadEmployees();
}

function updateLocalStats(employees) {
    const totalEl = document.getElementById('statTotal');
    const activeEl = document.getElementById('statActive');
    const inactiveEl = document.getElementById('statInactive');
    const deptEl = document.getElementById('statDept');

    if (totalEl) totalEl.textContent = employees.length;
    if (activeEl) activeEl.textContent = employees.filter(e => e.status?.toLowerCase() === 'active').length;
    if (inactiveEl) inactiveEl.textContent = employees.filter(e => e.status?.toLowerCase() === 'inactive').length;
    if (deptEl) {
        const uniqueDepts = new Set(employees.map(e => e.department).filter(Boolean));
        deptEl.textContent = uniqueDepts.size;
    }
}

/**
 * View Employee Details in Modal
 */
async function viewEmployeeDetails(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/${id}`);
        if (!response.ok) throw new Error('Employee not found');
        const emp = await response.json();

        document.getElementById('viewModalName').textContent = emp.fullName || `${emp.firstName} ${emp.lastName}`;
        document.getElementById('viewModalEmail').textContent = emp.email || 'N/A';
        document.getElementById('viewModalPhone').textContent = emp.phone || 'N/A';
        document.getElementById('viewModalDob').textContent = emp.dateOfBirth || 'N/A';
        document.getElementById('viewModalGender').textContent = emp.gender || 'N/A';
        document.getElementById('viewModalDept').textContent = emp.department || 'N/A';
        document.getElementById('viewModalDesignation').textContent = emp.designation || 'N/A';
        document.getElementById('viewModalSalary').textContent = emp.salary ? `₹${parseFloat(emp.salary).toLocaleString('en-IN')}` : 'N/A';
        document.getElementById('viewModalJoiningDate').textContent = emp.joiningDate || 'N/A';
        document.getElementById('viewModalAddress').textContent = emp.address || 'N/A';
        document.getElementById('viewModalStatus').innerHTML = `<span class="badge badge-status ${emp.status?.toLowerCase() === 'active' ? 'badge-status-active' : 'badge-status-inactive'}">${emp.status}</span>`;

        const viewModal = new bootstrap.Modal(document.getElementById('viewEmployeeModal'));
        viewModal.show();
    } catch (err) {
        showToast('Error fetching employee details', 'danger');
    }
}

/**
 * Delete Modal Flow
 */
function openDeleteModal(id, name) {
    if (currentRole !== 'ADMIN') {
        showToast('Delete permission is restricted to ADMIN role.', 'warning');
        return;
    }
    activeDeleteId = id;
    document.getElementById('deleteEmployeeName').textContent = name;
    const deleteModal = new bootstrap.Modal(document.getElementById('deleteConfirmModal'));
    deleteModal.show();
}

async function handleConfirmDelete() {
    if (!activeDeleteId) return;

    try {
        const res = await fetch(`${API_BASE_URL}/${activeDeleteId}`, {
            method: 'DELETE'
        });

        if (!res.ok) {
            const errData = await res.json().catch(() => ({}));
            throw new Error(errData.message || 'Delete operation failed');
        }

        const data = await res.json();
        showToast(data.message || 'Employee deleted successfully!', 'success');

        // Close modal
        const modalEl = document.getElementById('deleteConfirmModal');
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();

        activeDeleteId = null;
        loadEmployees();
        loadStats();
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

/**
 * Navigation to Edit
 */
function openEditEmployee(id) {
    window.location.href = `add-employee.html?id=${id}`;
}

/**
 * Dedicated Add / Edit Page Logic
 */
async function initFormPage() {
    const form = document.getElementById('employeeForm');
    const urlParams = new URLSearchParams(window.location.search);
    const employeeId = urlParams.get('id');

    const formTitle = document.getElementById('formTitle');
    const submitBtn = document.getElementById('submitBtn');

    if (employeeId) {
        // Edit Mode
        if (formTitle) formTitle.textContent = 'Edit Employee Profile';
        if (submitBtn) submitBtn.innerHTML = '<i class="bi bi-check-circle me-1"></i> Update Employee';
        await loadEmployeeDataForEdit(employeeId);
    } else {
        // Add Mode
        if (formTitle) formTitle.textContent = 'Add New Employee';
        if (submitBtn) submitBtn.innerHTML = '<i class="bi bi-person-plus me-1"></i> Save Employee';
    }

    form.addEventListener('submit', (e) => handleFormSubmit(e, employeeId));
}

async function loadEmployeeDataForEdit(id) {
    try {
        const res = await fetch(`${API_BASE_URL}/${id}`);
        if (!res.ok) throw new Error('Employee not found');
        const emp = await res.json();

        document.getElementById('employeeId').value = emp.id;
        document.getElementById('firstName').value = emp.firstName || '';
        document.getElementById('lastName').value = emp.lastName || '';
        document.getElementById('email').value = emp.email || '';
        document.getElementById('phone').value = emp.phone || '';
        document.getElementById('dateOfBirth').value = emp.dateOfBirth || '';
        document.getElementById('gender').value = emp.gender || '';
        document.getElementById('department').value = emp.department || '';
        document.getElementById('designation').value = emp.designation || '';
        document.getElementById('salary').value = emp.salary || '';
        document.getElementById('joiningDate').value = emp.joiningDate || '';
        document.getElementById('address').value = emp.address || '';
        document.getElementById('status').value = emp.status || 'Active';
    } catch (err) {
        showToast('Error loading employee profile for editing', 'danger');
    }
}

async function handleFormSubmit(e, employeeId) {
    e.preventDefault();
    clearFormErrors();

    // Client-side validation
    const payload = {
        firstName: document.getElementById('firstName').value.trim(),
        lastName: document.getElementById('lastName').value.trim(),
        email: document.getElementById('email').value.trim(),
        phone: document.getElementById('phone').value.trim() || null,
        dateOfBirth: document.getElementById('dateOfBirth').value || null,
        gender: document.getElementById('gender').value || null,
        department: document.getElementById('department').value.trim(),
        designation: document.getElementById('designation').value.trim() || null,
        salary: parseFloat(document.getElementById('salary').value),
        joiningDate: document.getElementById('joiningDate').value,
        address: document.getElementById('address').value.trim() || null,
        status: document.getElementById('status').value
    };

    let isValid = true;

    if (!payload.firstName) {
        showFieldError('firstName', 'First name is required');
        isValid = false;
    }
    if (!payload.lastName) {
        showFieldError('lastName', 'Last name is required');
        isValid = false;
    }
    if (!payload.email || !validateEmail(payload.email)) {
        showFieldError('email', 'A valid email address is required');
        isValid = false;
    }
    if (!payload.department) {
        showFieldError('department', 'Department is required');
        isValid = false;
    }
    if (isNaN(payload.salary) || payload.salary <= 0) {
        showFieldError('salary', 'Salary must be a positive number');
        isValid = false;
    }
    if (!payload.joiningDate) {
        showFieldError('joiningDate', 'Joining date is required');
        isValid = false;
    }
    if (!payload.status) {
        showFieldError('status', 'Status is required');
        isValid = false;
    }

    if (!isValid) return;

    // Send API Request
    const submitBtn = document.getElementById('submitBtn');
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Saving...';

    try {
        const isEdit = !!employeeId;
        const url = isEdit ? `${API_BASE_URL}/${employeeId}` : API_BASE_URL;
        const method = isEdit ? 'PUT' : 'POST';

        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const resData = await response.json();

        if (!response.ok) {
            if (response.status === 409) {
                showFieldError('email', resData.message || 'Email already exists');
                throw new Error(resData.message || 'Duplicate email conflict');
            } else if (response.status === 400 && resData.errors) {
                resData.errors.forEach(err => {
                    const parts = err.split(': ');
                    showFieldError(parts[0], parts[1] || err);
                });
                throw new Error(resData.message || 'Validation error');
            } else {
                throw new Error(resData.message || 'Operation failed');
            }
        }

        showToast(isEdit ? 'Employee profile updated successfully!' : 'New employee created successfully!', 'success');

        // Redirect back to dashboard after brief delay
        setTimeout(() => {
            window.location.href = 'index.html';
        }, 1200);

    } catch (err) {
        showToast(err.message, 'danger');
        submitBtn.disabled = false;
        submitBtn.innerHTML = employeeId ? '<i class="bi bi-check-circle me-1"></i> Update Employee' : '<i class="bi bi-person-plus me-1"></i> Save Employee';
    }
}

function showFieldError(fieldId, msg) {
    const input = document.getElementById(fieldId);
    if (!input) return;
    input.classList.add('is-invalid');
    const feedback = input.nextElementSibling;
    if (feedback && feedback.classList.contains('invalid-feedback')) {
        feedback.textContent = msg;
    }
}

function clearFormErrors() {
    document.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    document.querySelectorAll('.invalid-feedback').forEach(el => el.textContent = '');
}

function validateEmail(email) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
}

/**
 * Toast helper
 */
function showToast(message, type = 'success') {
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        document.body.appendChild(container);
    }

    const toastId = 'toast_' + Date.now();
    const bgClass = type === 'success' ? 'bg-success text-white' :
                    type === 'danger' ? 'bg-danger text-white' :
                    type === 'warning' ? 'bg-warning text-dark' : 'bg-primary text-white';

    const toastHtml = `
        <div id="${toastId}" class="toast align-items-center ${bgClass} border-0 mb-2 shadow" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body d-flex align-items-center gap-2">
                    <i class="bi ${type === 'success' ? 'bi-check-circle-fill' : type === 'danger' ? 'bi-exclamation-triangle-fill' : 'bi-info-circle-fill'}"></i>
                    <span>${message}</span>
                </div>
                <button type="button" class="btn-close ${type !== 'warning' ? 'btn-close-white' : ''} me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    `;

    container.insertAdjacentHTML('beforeend', toastHtml);
    const toastEl = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastEl, { delay: 4000 });
    toast.show();

    toastEl.addEventListener('hidden.bs.toast', () => {
        toastEl.remove();
    });
}

function showLoading(show) {
    const spinner = document.getElementById('tableSpinner');
    if (spinner) {
        spinner.style.display = show ? 'block' : 'none';
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function escapeQuotes(str) {
    if (!str) return '';
    return String(str).replace(/'/g, "\\'").replace(/"/g, '&quot;');
}

/**
 * Export current employee list to CSV
 */
function exportToCSV() {
    if (!currentEmployees || currentEmployees.length === 0) {
        showToast('No employee data available to export.', 'warning');
        return;
    }

    const headers = ["ID", "First Name", "Last Name", "Email", "Phone", "Gender", "Date of Birth", "Department", "Designation", "Salary", "Joining Date", "Status", "Address"];
    const rows = currentEmployees.map(e => [
        e.id,
        `"${e.firstName || ''}"`,
        `"${e.lastName || ''}"`,
        `"${e.email || ''}"`,
        `"${e.phone || ''}"`,
        `"${e.gender || ''}"`,
        `"${e.dateOfBirth || ''}"`,
        `"${e.department || ''}"`,
        `"${e.designation || ''}"`,
        e.salary || 0,
        `"${e.joiningDate || ''}"`,
        `"${e.status || ''}"`,
        `"${(e.address || '').replace(/"/g, '""')}"`
    ]);

    const csvContent = "data:text/csv;charset=utf-8," + [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `employees_export_${new Date().toISOString().slice(0,10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showToast('Employee table exported to CSV successfully!', 'success');
}
