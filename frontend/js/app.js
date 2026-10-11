/**
 * Employee Database - Employee Management System (EMS Pro)
 * Frontend Application Logic (Vanilla JS + Bootstrap 5)
 * Security Foundation & JWT Authentication Integration
 */

// Base API URLs
const API_BASE_URL = (window.location.port === '8081' || window.location.port === '8080')
    ? `${window.location.origin}/api/employees`
    : 'http://localhost:8081/api/employees';

const AUTH_API_BASE = (window.location.port === '8081' || window.location.port === '8080')
    ? `${window.location.origin}/api/auth`
    : 'http://localhost:8081/api/auth';

const DEPT_API_BASE = (window.location.port === '8081' || window.location.port === '8080')
    ? `${window.location.origin}/api/departments`
    : 'http://localhost:8081/api/departments';

const LEAVE_API_BASE = (window.location.port === '8081' || window.location.port === '8080')
    ? `${window.location.origin}/api/leaves`
    : 'http://localhost:8081/api/leaves';

const AUDIT_API_BASE = (window.location.port === '8081' || window.location.port === '8080')
    ? `${window.location.origin}/api/audit-logs`
    : 'http://localhost:8081/api/audit-logs';

// Authentication State Helpers
function getAuthToken() {
    return localStorage.getItem('ems_auth_token');
}

function getCurrentUser() {
    const raw = localStorage.getItem('ems_user');
    try {
        return raw ? JSON.parse(raw) : null;
    } catch (e) {
        return null;
    }
}

function isAuthenticated() {
    return !!getAuthToken();
}

function logout() {
    localStorage.removeItem('ems_auth_token');
    localStorage.removeItem('ems_user');
    localStorage.removeItem('ems_role');
    window.location.href = 'login.html?logout=true';
}

/**
 * Fetch wrapper automatically attaching JWT Bearer Authorization header
 * and handling 401 Unauthorized token expiry
 */
async function fetchWithAuth(url, options = {}) {
    const token = getAuthToken();
    if (!token) {
        window.location.href = 'login.html';
        return null;
    }

    options.headers = options.headers || {};
    if (!(options.body instanceof FormData) && !options.headers['Content-Type'] && options.body) {
        options.headers['Content-Type'] = 'application/json';
    }
    options.headers['Authorization'] = `Bearer ${token}`;

    try {
        const response = await fetch(url, options);

        if (response.status === 401) {
            localStorage.removeItem('ems_auth_token');
            localStorage.removeItem('ems_user');
            localStorage.removeItem('ems_role');
            window.location.href = 'login.html?session_expired=true';
            return response;
        }

        return response;
    } catch (err) {
        console.error('Fetch error:', err);
        throw err;
    }
}

// Global Application State
let currentEmployees = [];
let currentRole = 'ADMIN';
let activeDeleteId = null;
let activeTerminateId = null;
let activeTerminateJoiningDate = null;

// Initialize on DOM Ready
document.addEventListener('DOMContentLoaded', () => {
    const isLoginPage = window.location.pathname.endsWith('login.html');
    if (!isLoginPage && !isAuthenticated()) {
        window.location.href = 'login.html';
        return;
    }

    if (isLoginPage) return;

    const user = getCurrentUser();
    if (user) {
        currentRole = user.role || 'ADMIN';
    }

    initAuthUI();
    initEventListeners();
    initChangePasswordModal();
    initRegisterUserModal();
    initTerminateModal();

    if (document.getElementById('myLeavesTable')) {
        initLeavesPage();
    }

    if (document.getElementById('auditLogsTable')) {
        if (currentRole !== 'ADMIN') {
            showToast('Access Denied: Only administrators can access system audit logs.', 'warning');
            setTimeout(() => window.location.href = 'index.html', 1200);
            return;
        }
        initAuditLogsPage();
    }

    if (currentRole === 'EMPLOYEE') {
        initEmployeeRoleView(user);
    } else {
        // ADMIN or HR
        if (document.getElementById('departmentsTable')) {
            if (currentRole !== 'ADMIN') {
                showToast('Access Denied: Only ADMIN can manage departments.', 'warning');
                setTimeout(() => window.location.href = 'index.html', 1200);
                return;
            }
            loadDepartments();
        }
        if (document.getElementById('departmentFilter')) {
            populateDepartmentFilterDropdown();
        }
        if (document.getElementById('employeeTableBody')) {
            loadEmployees();
            loadStats();
        }
        if (document.getElementById('employeeForm')) {
            initFormPage();
        }
    }
});

/**
 * Sync navbar and interface elements with user authentication & role
 */
function initAuthUI() {
    const user = getCurrentUser();
    if (!user) return;

    const usernameDisplay = document.getElementById('currentUsernameDisplay');
    if (usernameDisplay) {
        usernameDisplay.textContent = user.username;
    }

    const roleBadge = document.getElementById('currentRoleDisplay');
    if (roleBadge) {
        roleBadge.textContent = user.role;
        roleBadge.className = 'badge ms-1 ' + (
            user.role === 'ADMIN' ? 'bg-danger' :
            user.role === 'HR' ? 'bg-primary' : 'bg-success'
        );
    }

    // Role-specific action buttons and modals
    if (user.role === 'ADMIN') {
        document.querySelectorAll('.admin-only').forEach(el => el.classList.remove('d-none'));
    }

    if (user.role === 'EMPLOYEE') {
        document.querySelectorAll('.admin-hr-only').forEach(el => el.classList.add('d-none'));
        document.querySelectorAll('.nav-admin-hr').forEach(el => el.classList.add('d-none'));
    }
}

/**
 * Handle Employee role access restrictions and self-service profile view
 */
async function initEmployeeRoleView(user) {
    // If an employee navigated directly to add/edit form, block access
    if (document.getElementById('employeeForm')) {
        showToast('Access Denied: Employees cannot create or edit personnel records.', 'warning');
        setTimeout(() => {
            window.location.href = 'profile.html';
        }, 1200);
        return;
    }

    // If an employee navigated directly to department management, block access
    if (document.getElementById('departmentsTable')) {
        showToast('Access Denied: Department management is restricted to administrators.', 'warning');
        setTimeout(() => {
            window.location.href = 'profile.html';
        }, 1200);
        return;
    }

    // Show Employee Role notice & details container on dashboard or directory
    const empContainer = document.getElementById('employeeRoleViewContainer');
    if (empContainer) {
        empContainer.classList.remove('d-none');
    }

    const empSpinner = document.getElementById('empRoleSpinner');
    const empDetails = document.getElementById('empRoleDetails');

    if (user && user.employeeId) {
        try {
            const res = await fetchWithAuth(`${API_BASE_URL}/${user.employeeId}`);
            if (!res.ok) throw new Error('Could not load profile');
            const emp = await res.json();

            if (empSpinner) empSpinner.classList.add('d-none');
            if (empDetails) empDetails.classList.remove('d-none');

            const nameEl = document.getElementById('empRoleName');
            const desigEl = document.getElementById('empRoleDesignation');
            const avatarEl = document.getElementById('empRoleAvatar');
            const idEl = document.getElementById('empRoleId');
            const emailEl = document.getElementById('empRoleEmail');
            const phoneEl = document.getElementById('empRolePhone');
            const deptEl = document.getElementById('empRoleDept');
            const joinEl = document.getElementById('empRoleJoin');
            const salaryEl = document.getElementById('empRoleSalary');
            const statusBadge = document.getElementById('empRoleStatusBadge');

            if (nameEl) nameEl.textContent = emp.fullName || `${emp.firstName} ${emp.lastName}`;
            if (desigEl) desigEl.textContent = emp.designation || 'N/A';
            if (avatarEl) avatarEl.textContent = `${emp.firstName ? emp.firstName[0] : ''}${emp.lastName ? emp.lastName[0] : ''}`.toUpperCase();
            if (idEl) idEl.textContent = `#${emp.id}`;
            if (emailEl) emailEl.textContent = emp.email || 'N/A';
            if (phoneEl) phoneEl.textContent = emp.phone || 'N/A';
            if (deptEl) deptEl.textContent = emp.department || 'N/A';
            if (joinEl) joinEl.textContent = emp.joiningDate ? new Date(emp.joiningDate).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' }) : 'N/A';
            if (salaryEl) salaryEl.textContent = emp.salary ? `₹${parseFloat(emp.salary).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` : 'N/A';
            if (statusBadge) statusBadge.textContent = emp.status || 'Active';

        } catch (err) {
            if (empSpinner) empSpinner.classList.add('d-none');
            showToast('Unable to load employee profile details.', 'danger');
        }
    } else {
        if (empSpinner) empSpinner.classList.add('d-none');
    }
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
        const response = await fetchWithAuth(`${API_BASE_URL}/stats`);
        if (!response || !response.ok) return;
        const stats = await response.json();

        const totalEl = document.getElementById('statTotal');
        const activeEl = document.getElementById('statActive');
        const inactiveEl = document.getElementById('statInactive');

        if (totalEl) totalEl.textContent = stats.total || 0;
        if (activeEl) activeEl.textContent = stats.active || 0;
        if (inactiveEl) inactiveEl.textContent = stats.terminated ? `${stats.inactive || 0} (${stats.terminated} Terminated)` : (stats.inactive || 0);
    } catch (err) {
        console.warn('Could not load server stats:', err);
    }
}

/**
 * Fetch all employees from backend API (ADMIN & HR)
 */
async function loadEmployees() {
    showLoading(true);
    try {
        const response = await fetchWithAuth(API_BASE_URL);
        if (!response) return;

        if (response.status === 403) {
            showToast('Access Denied: You do not have permission to view the employee directory.', 'danger');
            renderEmptyState('Access restricted to HR and ADMIN.');
            return;
        }

        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const employees = await response.json();
        currentEmployees = employees;
        renderEmployeeTable(employees);
    } catch (error) {
        console.error('Error loading employees:', error);
        showToast('Unable to connect to backend API.', 'danger');
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

        if (searchVal && (deptVal === 'ALL' || !deptVal) && (statusVal === 'ALL' || !statusVal)) {
            url = `${API_BASE_URL}/search?name=${encodeURIComponent(searchVal)}`;
        } else {
            const params = new URLSearchParams();
            if (deptVal && deptVal !== 'ALL') params.append('department', deptVal);
            if (statusVal && statusVal !== 'ALL') params.append('status', statusVal);
            url = `${API_BASE_URL}?${params.toString()}`;
        }

        const res = await fetchWithAuth(url);
        if (!res || !res.ok) throw new Error('Failed to filter employees');
        let data = await res.json();

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
 * Render Employee Table with role-aware action buttons
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
        const isTerminated = emp.status?.toUpperCase() === 'TERMINATED';
        const statusClass = emp.status?.toUpperCase() === 'ACTIVE'
            ? 'badge-status-active'
            : (isTerminated ? 'badge-status-terminated' : 'badge-status-inactive');
        const formattedSalary = emp.salary ? `₹${parseFloat(emp.salary).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` : 'N/A';
        const formattedDate = emp.joiningDate ? new Date(emp.joiningDate).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' }) : 'N/A';

        // Terminate button (ADMIN and HR for active employees)
        let terminateButtonHtml = '';
        if (currentRole === 'ADMIN' || currentRole === 'HR') {
            if (!isTerminated) {
                terminateButtonHtml = `
                    <button class="btn btn-sm btn-outline-warning btn-action" onclick="openTerminateModal(${emp.id}, '${escapeQuotes(emp.fullName || emp.firstName)}', '${emp.joiningDate || ''}')" title="Terminate Employee">
                        <i class="bi bi-person-x"></i>
                    </button>`;
            } else {
                terminateButtonHtml = `
                    <button class="btn btn-sm btn-outline-secondary btn-action" disabled title="Already Terminated">
                        <i class="bi bi-person-x-fill text-danger"></i>
                    </button>`;
            }
        }

        // Delete button restricted to ADMIN (HR cannot delete)
        const deleteButtonHtml = (currentRole === 'ADMIN')
            ? `<button class="btn btn-sm btn-outline-danger btn-action" onclick="openDeleteModal(${emp.id}, '${escapeQuotes(emp.fullName || emp.firstName)}')" title="Delete Employee">
                 <i class="bi bi-trash"></i>
               </button>`
            : `<button class="btn btn-sm btn-outline-secondary btn-action" disabled title="Delete restricted to ADMIN (HTTP 403)">
                 <i class="bi bi-lock-fill"></i>
               </button>`;

        return `
            <tr class="${isTerminated ? 'table-light text-muted' : ''}">
                <td class="text-muted fw-semibold">#${emp.id}</td>
                <td>
                    <div class="d-flex align-items-center">
                        <div class="employee-avatar ${isTerminated ? 'bg-danger-subtle text-danger' : ''}">${initials}</div>
                        <div>
                            <div class="fw-bold ${isTerminated ? 'text-secondary text-decoration-line-through' : 'text-dark'}">${escapeHtml(emp.fullName || `${emp.firstName} ${emp.lastName}`)}</div>
                            <div class="text-muted small">${escapeHtml(emp.email)}</div>
                        </div>
                    </div>
                </td>
                <td><span class="badge bg-light text-dark border">${escapeHtml(emp.department)}</span></td>
                <td class="text-secondary small fw-medium">${escapeHtml(emp.designation || 'N/A')}</td>
                <td><span class="badge-status ${statusClass}">${escapeHtml(emp.status)}</span></td>
                <td class="text-secondary small">${formattedDate}</td>
                <td class="fw-semibold text-dark">${formattedSalary}</td>
                <td class="text-center">
                    <div class="d-inline-flex gap-1">
                        <button class="btn btn-sm btn-outline-info btn-action" onclick="viewEmployeeDetails(${emp.id})" title="View Details">
                            <i class="bi bi-eye"></i>
                        </button>
                        <a href="add-employee.html?id=${emp.id}" class="btn btn-sm btn-outline-primary btn-action" title="Edit Employee">
                            <i class="bi bi-pencil"></i>
                        </a>
                        ${terminateButtonHtml}
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
            <td colspan="8" class="text-center py-5">
                <i class="bi bi-inbox text-muted" style="font-size: 2.5rem;"></i>
                <p class="text-muted mt-2 mb-0">${escapeHtml(message)}</p>
            </td>
        </tr>
    `;
}

/**
 * View Employee Details Modal
 */
async function viewEmployeeDetails(id) {
    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/${id}`);
        if (!response || !response.ok) throw new Error('Employee record could not be retrieved');
        const emp = await response.json();

        document.getElementById('viewModalId').textContent = `#${emp.id}`;
        document.getElementById('viewModalFullName').textContent = emp.fullName || `${emp.firstName} ${emp.lastName}`;
        document.getElementById('viewModalEmail').textContent = emp.email || 'N/A';
        document.getElementById('viewModalPhone').textContent = emp.phone || 'N/A';
        document.getElementById('viewModalDepartment').textContent = emp.department || 'N/A';
        document.getElementById('viewModalDesignation').textContent = emp.designation || 'N/A';
        document.getElementById('viewModalSalary').textContent = emp.salary ? `₹${parseFloat(emp.salary).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` : 'N/A';
        document.getElementById('viewModalDob').textContent = emp.dateOfBirth || 'N/A';
        document.getElementById('viewModalGender').textContent = emp.gender || 'N/A';
        document.getElementById('viewModalJoiningDate').textContent = emp.joiningDate ? new Date(emp.joiningDate).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' }) : 'N/A';
        document.getElementById('viewModalAddress').textContent = emp.address || 'N/A';

        const statusBadge = document.getElementById('viewModalStatusBadge');
        statusBadge.textContent = emp.status;
        statusBadge.className = 'badge ' + (
            emp.status === 'Active' ? 'bg-success' :
            (emp.status === 'Terminated' ? 'bg-danger' : 'bg-secondary')
        );

        const terminationSection = document.getElementById('viewModalTerminationSection');
        const terminationInfo = document.getElementById('viewModalTerminationInfo');
        if (terminationSection && terminationInfo) {
            if (emp.status === 'Terminated' && (emp.terminationDate || emp.terminationReason)) {
                terminationSection.classList.remove('d-none');
                terminationInfo.innerHTML = `<strong>Effective Date:</strong> ${emp.terminationDate || 'N/A'}<br><strong>Reason:</strong> ${escapeHtml(emp.terminationReason || 'N/A')}`;
            } else {
                terminationSection.classList.add('d-none');
            }
        }

        const initials = `${emp.firstName ? emp.firstName[0] : ''}${emp.lastName ? emp.lastName[0] : ''}`.toUpperCase();
        document.getElementById('viewModalAvatar').textContent = initials;

        const modal = new bootstrap.Modal(document.getElementById('viewEmployeeModal'));
        modal.show();
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

/**
 * Open Terminate Modal
 */
function openTerminateModal(id, name, joiningDate) {
    if (currentRole !== 'ADMIN' && currentRole !== 'HR') {
        showToast('Access Denied: Only ADMIN and HR can terminate employees.', 'danger');
        return;
    }
    activeTerminateId = id;
    activeTerminateJoiningDate = joiningDate;
    const nameEl = document.getElementById('terminateEmployeeName');
    if (nameEl) nameEl.textContent = name || `#${id}`;

    const dateInput = document.getElementById('terminateEffectiveDate');
    if (dateInput) {
        dateInput.value = new Date().toISOString().split('T')[0];
        if (joiningDate) {
            dateInput.min = joiningDate;
        }
    }
    const reasonInput = document.getElementById('terminateReason');
    if (reasonInput) reasonInput.value = '';

    const alertBox = document.getElementById('terminateAlert');
    if (alertBox) alertBox.classList.add('d-none');

    const modalEl = document.getElementById('terminateConfirmModal');
    if (modalEl) {
        const modal = new bootstrap.Modal(modalEl);
        modal.show();
    }
}

/**
 * Handle Employee Termination Submission via POST /api/employees/{id}/terminate
 */
function initTerminateModal() {
    const form = document.getElementById('terminateEmployeeForm');
    if (!form) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!activeTerminateId) return;

        const effectiveDate = document.getElementById('terminateEffectiveDate')?.value;
        const reason = document.getElementById('terminateReason')?.value.trim();
        const alertBox = document.getElementById('terminateAlert');

        if (!effectiveDate || !reason) {
            if (alertBox) {
                alertBox.className = 'alert alert-danger py-2 px-3 small';
                alertBox.textContent = 'Please provide both an effective termination date and reason.';
                alertBox.classList.remove('d-none');
            }
            return;
        }

        if (activeTerminateJoiningDate && effectiveDate < activeTerminateJoiningDate) {
            if (alertBox) {
                alertBox.className = 'alert alert-danger py-2 px-3 small';
                alertBox.textContent = `Termination effective date (${effectiveDate}) cannot be before employee joining date (${activeTerminateJoiningDate}).`;
                alertBox.classList.remove('d-none');
            }
            return;
        }

        const submitBtn = document.getElementById('confirmTerminateBtn');
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Terminating...';
        }

        try {
            const response = await fetchWithAuth(`${API_BASE_URL}/${activeTerminateId}/terminate`, {
                method: 'POST',
                body: JSON.stringify({ effectiveDate, reason })
            });

            if (!response) return;

            const resData = await response.json().catch(() => ({}));

            if (!response.ok) {
                throw new Error(resData.message || 'Failed to terminate employee');
            }

            showToast('Employee terminated successfully. System access revoked.', 'success');
            const modalEl = document.getElementById('terminateConfirmModal');
            if (modalEl) bootstrap.Modal.getInstance(modalEl)?.hide();

            loadEmployees();
            loadStats();
        } catch (err) {
            if (alertBox) {
                alertBox.className = 'alert alert-danger py-2 px-3 small';
                alertBox.textContent = err.message;
                alertBox.classList.remove('d-none');
            }
            showToast(err.message, 'danger');
        } finally {
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<i class="bi bi-person-x me-1"></i> Confirm Termination';
            }
        }
    });
}

/**
 * Open Delete Modal
 */
function openDeleteModal(id, name) {
    if (currentRole !== 'ADMIN') {
        showToast('Access Denied: Only ADMIN can delete employees.', 'danger');
        return;
    }
    activeDeleteId = id;
    const nameEl = document.getElementById('deleteEmployeeName');
    if (nameEl) nameEl.textContent = name || `#${id}`;

    const modal = new bootstrap.Modal(document.getElementById('deleteConfirmModal'));
    modal.show();
}

/**
 * Confirm and execute Delete via DELETE /api/employees/{id}
 */
async function handleConfirmDelete() {
    if (!activeDeleteId) return;

    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/${activeDeleteId}`, {
            method: 'DELETE'
        });

        if (!response) return;

        if (response.status === 403) {
            showToast('Access Denied: You do not have permission to delete employees (HTTP 403 Forbidden)', 'danger');
            const modalEl = document.getElementById('deleteConfirmModal');
            if (modalEl) bootstrap.Modal.getInstance(modalEl)?.hide();
            return;
        }

        if (!response.ok) {
            const errData = await response.json().catch(() => ({}));
            throw new Error(errData.message || 'Failed to delete employee record');
        }

        showToast('Employee record deleted successfully.', 'success');
        const modalEl = document.getElementById('deleteConfirmModal');
        if (modalEl) bootstrap.Modal.getInstance(modalEl)?.hide();

        loadEmployees();
        loadStats();
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

/**
 * Add / Edit Form Logic
 */
async function initFormPage() {
    const form = document.getElementById('employeeForm');
    if (!form) return;

    await populateDepartmentDropdown();

    const urlParams = new URLSearchParams(window.location.search);
    const employeeId = urlParams.get('id');

    const formTitle = document.getElementById('formTitle');
    const submitBtn = document.getElementById('submitBtn');

    if (employeeId) {
        if (formTitle) formTitle.textContent = 'Edit Employee Profile';
        if (submitBtn) submitBtn.innerHTML = '<i class="bi bi-check-circle me-1"></i> Update Employee';
        await loadEmployeeDataForEdit(employeeId);
    } else {
        if (formTitle) formTitle.textContent = 'Add New Employee';
        if (submitBtn) submitBtn.innerHTML = '<i class="bi bi-person-plus me-1"></i> Save Employee';
    }

    form.addEventListener('submit', (e) => handleFormSubmit(e, employeeId));
}

async function loadEmployeeDataForEdit(id) {
    try {
        const res = await fetchWithAuth(`${API_BASE_URL}/${id}`);
        if (!res || !res.ok) throw new Error('Employee not found or access denied');
        const emp = await res.json();

        document.getElementById('employeeId').value = emp.id;
        document.getElementById('firstName').value = emp.firstName || '';
        document.getElementById('lastName').value = emp.lastName || '';
        document.getElementById('email').value = emp.email || '';
        document.getElementById('phone').value = emp.phone || '';
        document.getElementById('dateOfBirth').value = emp.dateOfBirth || '';
        document.getElementById('gender').value = emp.gender || '';
        
        // Select Department dropdown by ID or fallback by name
        const deptSelect = document.getElementById('department');
        if (emp.departmentId) {
            deptSelect.value = emp.departmentId;
        } else if (emp.department) {
            for (let i = 0; i < deptSelect.options.length; i++) {
                if (deptSelect.options[i].text.toLowerCase() === emp.department.toLowerCase()) {
                    deptSelect.selectedIndex = i;
                    break;
                }
            }
        }

        document.getElementById('designation').value = emp.designation || '';
        document.getElementById('salary').value = emp.salary || '';
        document.getElementById('joiningDate').value = emp.joiningDate || '';
        document.getElementById('address').value = emp.address || '';
        document.getElementById('status').value = emp.status || 'Active';
    } catch (err) {
        showToast('Error loading employee profile for editing: ' + err.message, 'danger');
    }
}

async function handleFormSubmit(e, employeeId) {
    e.preventDefault();
    clearFormErrors();

    const deptSelect = document.getElementById('department');
    const selectedDeptId = deptSelect.value ? parseInt(deptSelect.value, 10) : null;
    const selectedDeptName = (deptSelect.selectedOptions && deptSelect.selectedOptions[0] && deptSelect.selectedOptions[0].value) 
        ? deptSelect.selectedOptions[0].text 
        : null;

    const payload = {
        firstName: document.getElementById('firstName').value.trim(),
        lastName: document.getElementById('lastName').value.trim(),
        email: document.getElementById('email').value.trim(),
        phone: document.getElementById('phone').value.trim() || null,
        dateOfBirth: document.getElementById('dateOfBirth').value || null,
        gender: document.getElementById('gender').value || null,
        departmentId: selectedDeptId,
        department: selectedDeptName,
        designation: document.getElementById('designation').value.trim() || null,
        salary: parseFloat(document.getElementById('salary').value),
        joiningDate: document.getElementById('joiningDate').value,
        address: document.getElementById('address').value.trim() || null,
        status: document.getElementById('status').value
    };

    let isValid = true;
    if (!payload.firstName) { showFieldError('firstName', 'First name cannot be blank'); isValid = false; }
    if (!payload.lastName) { showFieldError('lastName', 'Last name cannot be blank'); isValid = false; }
    if (!payload.email || !validateEmail(payload.email)) { showFieldError('email', 'A valid email address is required'); isValid = false; }
    if (!payload.departmentId) { showFieldError('department', 'Department is required'); isValid = false; }
    if (isNaN(payload.salary) || payload.salary <= 0 || payload.salary > 999999999.99) { showFieldError('salary', 'Salary must be a positive number up to 999,999,999.99'); isValid = false; }

    if (!payload.dateOfBirth) {
        showFieldError('dateOfBirth', 'Date of birth is required');
        isValid = false;
    } else {
        const dob = new Date(payload.dateOfBirth);
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        if (dob > today) {
            showFieldError('dateOfBirth', 'Date of birth cannot be in the future');
            isValid = false;
        }
    }

    if (!payload.joiningDate) {
        showFieldError('joiningDate', 'Date of joining is required');
        isValid = false;
    } else if (payload.dateOfBirth) {
        const dob = new Date(payload.dateOfBirth);
        const doj = new Date(payload.joiningDate);
        if (doj <= dob) {
            showFieldError('joiningDate', 'Date of joining must be strictly after date of birth');
            isValid = false;
        } else {
            const minJoiningDate = new Date(dob);
            minJoiningDate.setFullYear(minJoiningDate.getFullYear() + 18);
            if (doj < minJoiningDate) {
                showFieldError('joiningDate', 'Employee must be at least 18 years old on their joining date');
                isValid = false;
            }
        }
    }

    if (payload.phone && !/^[+0-9\s\-()]{7,20}$/.test(payload.phone)) {
        showFieldError('phone', 'Phone number must be a valid format between 7 and 20 digits');
        isValid = false;
    }

    if (!isValid) return;

    const submitBtn = document.getElementById('submitBtn');
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Saving...';

    try {
        const isEdit = !!employeeId;
        const url = isEdit ? `${API_BASE_URL}/${employeeId}` : API_BASE_URL;
        const method = isEdit ? 'PUT' : 'POST';

        const response = await fetchWithAuth(url, {
            method: method,
            body: JSON.stringify(payload)
        });

        if (!response) return;

        if (response.status === 403) {
            showToast('Access Denied: You do not have permission to modify employee profiles (HTTP 403 Forbidden)', 'danger');
            submitBtn.disabled = false;
            submitBtn.innerHTML = employeeId ? '<i class="bi bi-check-circle me-1"></i> Update Employee' : '<i class="bi bi-person-plus me-1"></i> Save Employee';
            return;
        }

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
        setTimeout(() => {
            window.location.href = 'index.html';
        }, 1200);

    } catch (err) {
        showToast(err.message, 'danger');
        submitBtn.disabled = false;
        submitBtn.innerHTML = employeeId ? '<i class="bi bi-check-circle me-1"></i> Update Employee' : '<i class="bi bi-person-plus me-1"></i> Save Employee';
    }
}

/**
 * Change Password Modal Handler (Authenticated User)
 */
function initChangePasswordModal() {
    const form = document.getElementById('changePasswordForm');
    if (!form) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const currentPassword = document.getElementById('currentPasswordInput')?.value;
        const newPassword = document.getElementById('newPasswordInput')?.value;
        const confirmPassword = document.getElementById('confirmPasswordInput')?.value;
        const alertBox = document.getElementById('changePasswordAlert');

        if (!currentPassword || !newPassword) {
            if (alertBox) {
                alertBox.className = 'alert alert-danger py-2 px-3 small';
                alertBox.textContent = 'Please provide both current and new password.';
                alertBox.classList.remove('d-none');
            }
            return;
        }

        if (newPassword.length < 6) {
            if (alertBox) {
                alertBox.className = 'alert alert-danger py-2 px-3 small';
                alertBox.textContent = 'New password must be at least 6 characters long.';
                alertBox.classList.remove('d-none');
            }
            return;
        }

        if (newPassword !== confirmPassword) {
            if (alertBox) {
                alertBox.className = 'alert alert-danger py-2 px-3 small';
                alertBox.textContent = 'New password and confirmation do not match.';
                alertBox.classList.remove('d-none');
            }
            return;
        }

        const submitBtn = document.getElementById('submitChangePasswordBtn');
        submitBtn.disabled = true;

        try {
            const res = await fetchWithAuth(`${AUTH_API_BASE}/change-password`, {
                method: 'PUT',
                body: JSON.stringify({ currentPassword, newPassword })
            });

            const data = await res.json();
            if (!res.ok) {
                throw new Error(data.message || 'Failed to change password');
            }

            if (alertBox) {
                alertBox.className = 'alert alert-success py-2 px-3 small';
                alertBox.textContent = 'Password updated successfully!';
                alertBox.classList.remove('d-none');
            }
            showToast('Password updated successfully!', 'success');

            setTimeout(() => {
                const modalEl = document.getElementById('changePasswordModal');
                if (modalEl) bootstrap.Modal.getInstance(modalEl)?.hide();
                form.reset();
                if (alertBox) alertBox.classList.add('d-none');
                submitBtn.disabled = false;
            }, 1000);

        } catch (err) {
            if (alertBox) {
                alertBox.className = 'alert alert-danger py-2 px-3 small';
                alertBox.textContent = err.message;
                alertBox.classList.remove('d-none');
            }
            submitBtn.disabled = false;
        }
    });
}

/**
 * Register User Modal Handler (ADMIN only)
 */
function initRegisterUserModal() {
    const form = document.getElementById('registerUserForm');
    if (!form) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('regUsernameInput')?.value.trim();
        const email = document.getElementById('regEmailInput')?.value.trim();
        const password = document.getElementById('regPasswordInput')?.value;
        const role = document.getElementById('regRoleSelect')?.value;
        const employeeIdVal = document.getElementById('regEmployeeIdInput')?.value.trim();
        const employeeId = employeeIdVal ? parseInt(employeeIdVal, 10) : null;
        const alertBox = document.getElementById('registerUserAlert');

        const submitBtn = document.getElementById('submitRegisterUserBtn');
        submitBtn.disabled = true;

        try {
            const res = await fetchWithAuth(`${AUTH_API_BASE}/register`, {
                method: 'POST',
                body: JSON.stringify({ username, email, password, role, employeeId })
            });

            const data = await res.json();
            if (!res.ok) {
                throw new Error(data.message || 'Failed to register user');
            }

            if (alertBox) {
                alertBox.className = 'alert alert-success py-2 px-3 small';
                alertBox.textContent = `User '${data.username}' registered successfully!`;
                alertBox.classList.remove('d-none');
            }
            showToast(`User ${data.username} registered with role ${data.role}!`, 'success');

            setTimeout(() => {
                const modalEl = document.getElementById('registerUserModal');
                if (modalEl) bootstrap.Modal.getInstance(modalEl)?.hide();
                form.reset();
                if (alertBox) alertBox.classList.add('d-none');
                submitBtn.disabled = false;
            }, 1200);

        } catch (err) {
            if (alertBox) {
                alertBox.className = 'alert alert-danger py-2 px-3 small';
                alertBox.textContent = err.message;
                alertBox.classList.remove('d-none');
            }
            submitBtn.disabled = false;
        }
    });
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

/**
 * =========================================================================
 * Phase 2: Department Management & Dropdown Integrations
 * =========================================================================
 */

let allDepartments = [];
let deleteDeptTargetId = null;

async function populateDepartmentDropdown(selectedId = null) {
    const deptSelect = document.getElementById('department');
    if (!deptSelect) return;

    try {
        const res = await fetchWithAuth(DEPT_API_BASE);
        if (!res || !res.ok) {
            deptSelect.innerHTML = '<option value="">Failed to load departments</option>';
            return;
        }
        const depts = await res.json();
        deptSelect.innerHTML = '<option value="">Select Department</option>';
        depts.forEach(d => {
            const opt = document.createElement('option');
            opt.value = d.id;
            opt.textContent = d.name;
            if (selectedId && (selectedId === d.id || selectedId === d.name)) {
                opt.selected = true;
            }
            deptSelect.appendChild(opt);
        });
    } catch (err) {
        console.error('Error fetching departments:', err);
        deptSelect.innerHTML = '<option value="">Error loading departments</option>';
    }
}

async function populateDepartmentFilterDropdown() {
    const deptFilter = document.getElementById('departmentFilter');
    if (!deptFilter) return;

    try {
        const res = await fetchWithAuth(DEPT_API_BASE);
        if (!res || !res.ok) return;
        const depts = await res.json();

        const currentVal = deptFilter.value || 'ALL';
        deptFilter.innerHTML = '<option value="ALL">All Departments</option>';
        depts.forEach(d => {
            const opt = document.createElement('option');
            opt.value = d.name;
            opt.textContent = d.name;
            deptFilter.appendChild(opt);
        });
        if (currentVal && Array.from(deptFilter.options).some(o => o.value === currentVal)) {
            deptFilter.value = currentVal;
        }
    } catch (err) {
        console.warn('Could not populate department filter:', err);
    }
}

async function loadDepartments() {
    const spinner = document.getElementById('deptLoadingSpinner');
    const table = document.getElementById('departmentsTable');
    const emptyState = document.getElementById('deptEmptyState');

    if (spinner) spinner.classList.remove('d-none');
    if (table) table.classList.add('d-none');
    if (emptyState) emptyState.classList.add('d-none');

    try {
        const res = await fetchWithAuth(DEPT_API_BASE);
        if (!res || !res.ok) {
            throw new Error(res ? `Status ${res.status}` : 'Network error');
        }
        allDepartments = await res.json();

        updateDepartmentStats(allDepartments);

        if (!allDepartments || allDepartments.length === 0) {
            if (spinner) spinner.classList.add('d-none');
            if (emptyState) emptyState.classList.remove('d-none');
            return;
        }

        renderDepartmentsTable(allDepartments);

        if (spinner) spinner.classList.add('d-none');
        if (table) table.classList.remove('d-none');
    } catch (err) {
        if (spinner) spinner.classList.add('d-none');
        showToast('Error loading departments: ' + err.message, 'danger');
    }
}

function updateDepartmentStats(depts) {
    const totalEl = document.getElementById('statDeptTotal');
    const empEl = document.getElementById('statDeptEmployees');
    const largestEl = document.getElementById('statDeptLargest');

    if (!depts || depts.length === 0) {
        if (totalEl) totalEl.textContent = '0';
        if (empEl) empEl.textContent = '0';
        if (largestEl) largestEl.textContent = 'None';
        return;
    }

    if (totalEl) totalEl.textContent = depts.length;

    let totalEmployees = 0;
    let largestDept = depts[0];

    depts.forEach(d => {
        const count = d.employeeCount || 0;
        totalEmployees += count;
        if (count > (largestDept.employeeCount || 0)) {
            largestDept = d;
        }
    });

    if (empEl) empEl.textContent = totalEmployees;
    if (largestEl) largestEl.textContent = `${largestDept.name} (${largestDept.employeeCount || 0})`;
}

function renderDepartmentsTable(depts) {
    const tbody = document.getElementById('departmentsTableBody');
    if (!tbody) return;

    if (!depts || depts.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">No matching departments found.</td></tr>`;
        return;
    }

    tbody.innerHTML = depts.map(d => {
        const createdStr = d.createdAt ? formatDate(d.createdAt.split('T')[0]) : '—';
        const empCount = d.employeeCount || 0;
        const countBadge = empCount > 0
            ? `<span class="badge bg-primary px-2 py-1"><i class="bi bi-people-fill me-1"></i>${empCount}</span>`
            : `<span class="badge bg-light text-muted border px-2 py-1">0</span>`;

        return `
            <tr>
                <td class="ps-4 fw-semibold text-muted">#${d.id}</td>
                <td class="fw-bold text-dark">
                    <i class="bi bi-building me-1 text-secondary"></i>${escapeHtml(d.name)}
                </td>
                <td class="text-muted small">${escapeHtml(d.description || '—')}</td>
                <td class="text-center">${countBadge}</td>
                <td class="text-secondary small">${createdStr}</td>
                <td class="text-end pe-4">
                    <button class="btn btn-sm btn-outline-warning me-1" 
                            onclick="openEditDepartmentModal(${d.id}, '${escapeQuotes(d.name)}', '${escapeQuotes(d.description || '')}')" 
                            title="Edit Department">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger" 
                            onclick="openDeleteDepartmentModal(${d.id}, '${escapeQuotes(d.name)}', ${empCount})" 
                            title="Delete Department">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function filterDepartmentsTable() {
    const query = document.getElementById('deptSearchInput')?.value.trim().toLowerCase() || '';
    if (!query) {
        renderDepartmentsTable(allDepartments);
        return;
    }

    const filtered = allDepartments.filter(d =>
        (d.name && d.name.toLowerCase().includes(query)) ||
        (d.description && d.description.toLowerCase().includes(query)) ||
        String(d.id).includes(query)
    );
    renderDepartmentsTable(filtered);
}

async function handleCreateDepartment(e) {
    e.preventDefault();
    const alertEl = document.getElementById('addDeptAlert');
    const saveBtn = document.getElementById('btnSaveDepartment');
    if (alertEl) alertEl.classList.add('d-none');

    const name = document.getElementById('newDeptName').value.trim();
    const description = document.getElementById('newDeptDesc').value.trim();

    if (!name) {
        if (alertEl) {
            alertEl.textContent = 'Department name cannot be blank.';
            alertEl.classList.remove('d-none');
        }
        return;
    }

    try {
        if (saveBtn) saveBtn.disabled = true;
        const res = await fetchWithAuth(DEPT_API_BASE, {
            method: 'POST',
            body: JSON.stringify({ name, description })
        });

        if (res.status === 409) {
            const err = await res.json();
            throw new Error(err.message || `Department '${name}' already exists.`);
        }

        if (!res.ok) {
            const err = await res.json();
            const msg = err.errors && err.errors.length ? err.errors.join(', ') : (err.message || 'Validation failed');
            throw new Error(msg);
        }

        const modalEl = document.getElementById('addDepartmentModal');
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();

        document.getElementById('addDepartmentForm').reset();
        showToast(`Department '${name}' created successfully!`, 'success');
        await loadDepartments();
    } catch (err) {
        if (alertEl) {
            alertEl.textContent = err.message;
            alertEl.classList.remove('d-none');
        } else {
            showToast(err.message, 'danger');
        }
    } finally {
        if (saveBtn) saveBtn.disabled = false;
    }
}

function openEditDepartmentModal(id, name, description) {
    const alertEl = document.getElementById('editDeptAlert');
    if (alertEl) alertEl.classList.add('d-none');

    document.getElementById('editDeptId').value = id;
    document.getElementById('editDeptName').value = name;
    document.getElementById('editDeptDesc').value = description;

    const modalEl = document.getElementById('editDepartmentModal');
    const modal = new bootstrap.Modal(modalEl);
    modal.show();
}

async function handleUpdateDepartment(e) {
    e.preventDefault();
    const alertEl = document.getElementById('editDeptAlert');
    const updateBtn = document.getElementById('btnUpdateDepartment');
    if (alertEl) alertEl.classList.add('d-none');

    const id = document.getElementById('editDeptId').value;
    const name = document.getElementById('editDeptName').value.trim();
    const description = document.getElementById('editDeptDesc').value.trim();

    if (!name) {
        if (alertEl) {
            alertEl.textContent = 'Department name cannot be blank.';
            alertEl.classList.remove('d-none');
        }
        return;
    }

    try {
        if (updateBtn) updateBtn.disabled = true;
        const res = await fetchWithAuth(`${DEPT_API_BASE}/${id}`, {
            method: 'PUT',
            body: JSON.stringify({ name, description })
        });

        if (res.status === 409) {
            const err = await res.json();
            throw new Error(err.message || `Department with name '${name}' already exists.`);
        }

        if (!res.ok) {
            const err = await res.json();
            const msg = err.errors && err.errors.length ? err.errors.join(', ') : (err.message || 'Update failed');
            throw new Error(msg);
        }

        const modalEl = document.getElementById('editDepartmentModal');
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();

        showToast(`Department '${name}' updated successfully!`, 'success');
        await loadDepartments();
    } catch (err) {
        if (alertEl) {
            alertEl.textContent = err.message;
            alertEl.classList.remove('d-none');
        } else {
            showToast(err.message, 'danger');
        }
    } finally {
        if (updateBtn) updateBtn.disabled = false;
    }
}

function openDeleteDepartmentModal(id, name, employeeCount) {
    deleteDeptTargetId = id;
    const alertEl = document.getElementById('deleteDeptAlert');
    const hasEmployeesAlert = document.getElementById('deleteDeptHasEmployeesAlert');
    const confirmBtn = document.getElementById('btnConfirmDeleteDept');
    const nameSpan = document.getElementById('deleteDeptName');
    const empCountSpan = document.getElementById('deleteDeptEmpCount');

    if (alertEl) alertEl.classList.add('d-none');
    if (nameSpan) nameSpan.textContent = name;
    if (empCountSpan) empCountSpan.textContent = employeeCount;

    if (employeeCount > 0) {
        if (hasEmployeesAlert) hasEmployeesAlert.classList.remove('d-none');
        if (confirmBtn) {
            confirmBtn.disabled = true;
            confirmBtn.title = 'Cannot delete a department with assigned employees';
        }
    } else {
        if (hasEmployeesAlert) hasEmployeesAlert.classList.add('d-none');
        if (confirmBtn) {
            confirmBtn.disabled = false;
            confirmBtn.title = '';
        }
    }

    const modalEl = document.getElementById('deleteDepartmentModal');
    const modal = new bootstrap.Modal(modalEl);
    modal.show();
}

async function confirmDeleteDepartment() {
    if (!deleteDeptTargetId) return;

    const alertEl = document.getElementById('deleteDeptAlert');
    const confirmBtn = document.getElementById('btnConfirmDeleteDept');
    if (alertEl) alertEl.classList.add('d-none');

    try {
        if (confirmBtn) confirmBtn.disabled = true;
        const res = await fetchWithAuth(`${DEPT_API_BASE}/${deleteDeptTargetId}`, {
            method: 'DELETE'
        });

        if (res.status === 409) {
            const err = await res.json();
            throw new Error(err.message || 'Cannot delete department with assigned employees.');
        }

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Failed to delete department.');
        }

        const modalEl = document.getElementById('deleteDepartmentModal');
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();

        showToast('Department deleted successfully.', 'success');
        deleteDeptTargetId = null;
        await loadDepartments();
    } catch (err) {
        if (alertEl) {
            alertEl.textContent = err.message;
            alertEl.classList.remove('d-none');
        } else {
            showToast(err.message, 'danger');
        }
    } finally {
        if (confirmBtn) confirmBtn.disabled = false;
    }
}

/* ==========================================================================
   Phase 3: Leave Management Module Frontend Logic
   ========================================================================== */

let myLeavesList = [];
let allLeavesList = [];
let cancelLeaveTargetId = null;
let reviewLeaveTargetId = null;

async function initLeavesPage() {
    const user = getCurrentUser();

    // Set today as min date for leave request inputs
    const todayStr = new Date().toISOString().split('T')[0];
    const startDateInput = document.getElementById('leaveStartDate');
    const endDateInput = document.getElementById('leaveEndDate');
    if (startDateInput) {
        startDateInput.min = todayStr;
        startDateInput.addEventListener('change', () => {
            if (endDateInput) {
                endDateInput.min = startDateInput.value;
                if (endDateInput.value && endDateInput.value < startDateInput.value) {
                    endDateInput.value = startDateInput.value;
                }
            }
        });
    }

    // If Admin/HR, enable employee selection dropdown in leave request modal
    if (user && (user.role === 'ADMIN' || user.role === 'HR')) {
        const adminEmpGroup = document.getElementById('adminSelectEmployeeGroup');
        if (adminEmpGroup) {
            adminEmpGroup.classList.remove('d-none');
            populateLeaveEmployeeSelect();
        }
    }

    // Load user's own leaves
    await loadMyLeaves();

    // If ADMIN or HR, load all approvals
    if (user && (user.role === 'ADMIN' || user.role === 'HR')) {
        await loadAllLeaves();
    } else {
        // Hide management approvals tab for EMPLOYEE
        const approvalsTab = document.getElementById('tabApprovalsItem');
        if (approvalsTab) approvalsTab.classList.add('d-none');
    }

    updateLeaveDashboardMetrics();
}

async function populateLeaveEmployeeSelect() {
    const select = document.getElementById('leaveTargetEmployeeId');
    if (!select) return;

    try {
        const res = await fetchWithAuth(API_BASE_URL);
        if (!res.ok) return;
        const employees = await res.json();
        select.innerHTML = '<option value="">-- Select Employee --</option>';
        employees
            .filter(e => e.status !== 'TERMINATED')
            .forEach(e => {
                const opt = document.createElement('option');
                opt.value = e.id;
                opt.textContent = `${e.fullName || `${e.firstName} ${e.lastName}`} (#${e.id}) - ${e.department || 'No Dept'}`;
                select.appendChild(opt);
            });
    } catch (err) {
        console.warn('Could not populate employee select for leave request:', err);
    }
}

async function loadMyLeaves() {
    const tbody = document.getElementById('myLeavesTableBody');
    if (!tbody) return;

    try {
        const res = await fetchWithAuth(`${LEAVE_API_BASE}/my-leaves`);
        if (!res.ok) {
            throw new Error('Failed to load leave requests.');
        }
        myLeavesList = await res.json();
        renderMyLeavesTable(myLeavesList);
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-danger"><i class="bi bi-exclamation-triangle me-1"></i> ${err.message}</td></tr>`;
    }
}

function renderMyLeavesTable(leaves) {
    const tbody = document.getElementById('myLeavesTableBody');
    if (!tbody) return;

    if (!leaves || leaves.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center py-5 text-muted">
            <i class="bi bi-calendar2-x fs-2 d-block mb-2 text-secondary"></i>
            No leave requests found. Click <strong>Request Leave</strong> to submit one.
        </td></tr>`;
        return;
    }

    tbody.innerHTML = leaves.map(leave => {
        const statusBadge = getLeaveStatusBadge(leave.status);
        const days = calculateDaysBetween(leave.startDate, leave.endDate);
        const canCancel = leave.status === 'PENDING';

        const reviewInfo = leave.reviewedBy
            ? `<span class="small text-secondary">${leave.reviewedBy}</span>${leave.reviewComments ? `<div class="small text-muted fst-italic">"${escapeHtml(leave.reviewComments)}"</div>` : ''}`
            : '<span class="text-muted small">Pending</span>';

        return `
            <tr>
                <td class="ps-3 fw-bold text-muted">#${leave.id}</td>
                <td><span class="badge bg-light text-dark border">${leave.leaveType}</span></td>
                <td>
                    <div class="fw-semibold">${formatDate(leave.startDate)}</div>
                    <small class="text-muted">to ${formatDate(leave.endDate)}</small>
                </td>
                <td><span class="badge bg-secondary-subtle text-dark">${days} day${days > 1 ? 's' : ''}</span></td>
                <td><span class="small" title="${escapeHtml(leave.reason || '')}">${escapeHtml(truncate(leave.reason, 35))}</span></td>
                <td>${statusBadge}</td>
                <td>${reviewInfo}</td>
                <td class="text-end pe-3">
                    ${canCancel ? `
                        <button class="btn btn-sm btn-outline-danger" onclick="openCancelLeaveModal(${leave.id})" title="Cancel Leave Request">
                            <i class="bi bi-x-circle me-1"></i> Cancel
                        </button>
                    ` : '<span class="text-muted small">-</span>'}
                </td>
            </tr>
        `;
    }).join('');
}

async function loadAllLeaves() {
    const tbody = document.getElementById('allLeavesTableBody');
    if (!tbody) return;

    const statusFilter = document.getElementById('leaveApprovalStatusFilter');
    const status = statusFilter ? statusFilter.value : '';

    const url = status ? `${LEAVE_API_BASE}?status=${status}` : LEAVE_API_BASE;

    try {
        const res = await fetchWithAuth(url);
        if (!res.ok) {
            throw new Error('Failed to load leave approval requests.');
        }
        allLeavesList = await res.json();
        renderAllLeavesTable(allLeavesList);
        updatePendingApprovalsBadge();
        updateLeaveDashboardMetrics();
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center py-4 text-danger"><i class="bi bi-exclamation-triangle me-1"></i> ${err.message}</td></tr>`;
    }
}

function renderAllLeavesTable(leaves) {
    const tbody = document.getElementById('allLeavesTableBody');
    if (!tbody) return;

    if (!leaves || leaves.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center py-5 text-muted">
            <i class="bi bi-inbox fs-2 d-block mb-2 text-secondary"></i>
            No leave requests matching the criteria.
        </td></tr>`;
        return;
    }

    const currentUser = getCurrentUser();

    tbody.innerHTML = leaves.map(leave => {
        const statusBadge = getLeaveStatusBadge(leave.status);
        const days = calculateDaysBetween(leave.startDate, leave.endDate);
        const isPending = leave.status === 'PENDING';
        const isSelf = currentUser && currentUser.employeeId && currentUser.employeeId === leave.employeeId;

        const reviewInfo = leave.reviewedBy
            ? `<span class="small text-secondary">${leave.reviewedBy}</span>${leave.reviewComments ? `<div class="small text-muted fst-italic">"${escapeHtml(leave.reviewComments)}"</div>` : ''}`
            : '<span class="text-muted small">Pending</span>';

        return `
            <tr>
                <td class="ps-3 fw-bold text-muted">#${leave.id}</td>
                <td>
                    <div class="fw-bold">${escapeHtml(leave.employeeName || 'Unknown')}</div>
                    <div class="small text-muted">${escapeHtml(leave.departmentName || leave.employeeEmail || '')}</div>
                </td>
                <td><span class="badge bg-light text-dark border">${leave.leaveType}</span></td>
                <td>
                    <div class="fw-semibold">${formatDate(leave.startDate)}</div>
                    <small class="text-muted">to ${formatDate(leave.endDate)}</small>
                </td>
                <td><span class="badge bg-secondary-subtle text-dark">${days} day${days > 1 ? 's' : ''}</span></td>
                <td><span class="small" title="${escapeHtml(leave.reason || '')}">${escapeHtml(truncate(leave.reason, 30))}</span></td>
                <td>${statusBadge}</td>
                <td>${reviewInfo}</td>
                <td class="text-end pe-3">
                    ${isPending ? `
                        <button class="btn btn-sm btn-primary-custom" onclick="openReviewLeaveModal(${leave.id})" ${isSelf ? 'title="Self-review is prohibited" disabled' : ''}>
                            <i class="bi bi-pencil-square me-1"></i> Review
                        </button>
                    ` : '<span class="text-muted small">-</span>'}
                </td>
            </tr>
        `;
    }).join('');
}

function filterAllLeaves() {
    const searchInput = document.getElementById('leaveApprovalSearchInput');
    const query = searchInput ? searchInput.value.toLowerCase().trim() : '';

    if (!query) {
        renderAllLeavesTable(allLeavesList);
        return;
    }

    const filtered = allLeavesList.filter(l => {
        const name = (l.employeeName || '').toLowerCase();
        const email = (l.employeeEmail || '').toLowerCase();
        const reason = (l.reason || '').toLowerCase();
        const dept = (l.departmentName || '').toLowerCase();
        return name.includes(query) || email.includes(query) || reason.includes(query) || dept.includes(query);
    });

    renderAllLeavesTable(filtered);
}

function updatePendingApprovalsBadge() {
    const badge = document.getElementById('pendingApprovalsBadge');
    if (!badge) return;
    const pendingCount = allLeavesList.filter(l => l.status === 'PENDING').length;
    badge.textContent = pendingCount;
    badge.className = `badge ms-1 ${pendingCount > 0 ? 'bg-warning text-dark' : 'bg-secondary'}`;
}

function updateLeaveDashboardMetrics() {
    const user = getCurrentUser();
    const sourceList = (user && (user.role === 'ADMIN' || user.role === 'HR') && allLeavesList.length > 0)
        ? allLeavesList
        : myLeavesList;

    const totalEl = document.getElementById('statLeaveTotal');
    const pendingEl = document.getElementById('statLeavePending');
    const approvedEl = document.getElementById('statLeaveApproved');
    const rejectedEl = document.getElementById('statLeaveRejected');

    if (!totalEl) return;

    const total = sourceList.length;
    const pending = sourceList.filter(l => l.status === 'PENDING').length;
    const approved = sourceList.filter(l => l.status === 'APPROVED').length;
    const rejected = sourceList.filter(l => l.status === 'REJECTED' || l.status === 'CANCELLED').length;

    totalEl.textContent = total;
    if (pendingEl) pendingEl.textContent = pending;
    if (approvedEl) approvedEl.textContent = approved;
    if (rejectedEl) rejectedEl.textContent = rejected;
}

function getLeaveStatusBadge(status) {
    switch (status) {
        case 'PENDING':
            return '<span class="badge bg-warning text-dark"><i class="bi bi-hourglass-split me-1"></i>Pending</span>';
        case 'APPROVED':
            return '<span class="badge bg-success"><i class="bi bi-check-circle me-1"></i>Approved</span>';
        case 'REJECTED':
            return '<span class="badge bg-danger"><i class="bi bi-x-circle me-1"></i>Rejected</span>';
        case 'CANCELLED':
            return '<span class="badge bg-secondary"><i class="bi bi-dash-circle me-1"></i>Cancelled</span>';
        default:
            return `<span class="badge bg-light text-dark">${status}</span>`;
    }
}

function calculateDaysBetween(startDateStr, endDateStr) {
    if (!startDateStr || !endDateStr) return 1;
    const s = new Date(startDateStr);
    const e = new Date(endDateStr);
    const diffTime = e - s;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
    return diffDays > 0 ? diffDays : 1;
}

async function handleLeaveSubmit(e) {
    e.preventDefault();

    const alertEl = document.getElementById('requestLeaveAlert');
    const submitBtn = document.getElementById('btnSubmitLeave');
    if (alertEl) alertEl.classList.add('d-none');

    const leaveType = document.getElementById('leaveTypeSelect').value;
    const startDate = document.getElementById('leaveStartDate').value;
    const endDate = document.getElementById('leaveEndDate').value;
    const reason = document.getElementById('leaveReason').value.trim();

    const empSelect = document.getElementById('leaveTargetEmployeeId');
    const employeeId = (empSelect && empSelect.value) ? parseInt(empSelect.value, 10) : null;

    if (!startDate || !endDate || !reason) {
        if (alertEl) {
            alertEl.textContent = 'Please fill out all required fields.';
            alertEl.classList.remove('d-none');
        }
        return;
    }

    if (endDate < startDate) {
        if (alertEl) {
            alertEl.textContent = 'Leave end date cannot be earlier than start date.';
            alertEl.classList.remove('d-none');
        }
        return;
    }

    const payload = {
        leaveType,
        startDate,
        endDate,
        reason
    };
    if (employeeId) {
        payload.employeeId = employeeId;
    }

    try {
        if (submitBtn) submitBtn.disabled = true;
        const res = await fetchWithAuth(LEAVE_API_BASE, {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        if (res.status === 409) {
            const err = await res.json();
            throw new Error(err.message || 'Overlapping leave request exists for the selected dates.');
        }

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Failed to submit leave request.');
        }

        const modalEl = document.getElementById('requestLeaveModal');
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();

        document.getElementById('requestLeaveForm').reset();
        showToast('Leave request submitted successfully.', 'success');

        await loadMyLeaves();
        const user = getCurrentUser();
        if (user && (user.role === 'ADMIN' || user.role === 'HR')) {
            await loadAllLeaves();
        }
        updateLeaveDashboardMetrics();
    } catch (err) {
        if (alertEl) {
            alertEl.textContent = err.message;
            alertEl.classList.remove('d-none');
        } else {
            showToast(err.message, 'danger');
        }
    } finally {
        if (submitBtn) submitBtn.disabled = false;
    }
}

function openReviewLeaveModal(id) {
    const leave = allLeavesList.find(l => l.id === id);
    if (!leave) return;

    reviewLeaveTargetId = id;
    const alertEl = document.getElementById('reviewLeaveAlert');
    if (alertEl) alertEl.classList.add('d-none');

    const nameEl = document.getElementById('reviewEmpName');
    const typeEl = document.getElementById('reviewType');
    const datesEl = document.getElementById('reviewDates');
    const durEl = document.getElementById('reviewDuration');
    const reasonEl = document.getElementById('reviewReason');
    const confirmBtn = document.getElementById('btnConfirmReview');

    const days = calculateDaysBetween(leave.startDate, leave.endDate);

    if (nameEl) nameEl.textContent = `${leave.employeeName} (${leave.departmentName || 'No Dept'})`;
    if (typeEl) typeEl.textContent = leave.leaveType;
    if (datesEl) datesEl.textContent = `${formatDate(leave.startDate)} to ${formatDate(leave.endDate)}`;
    if (durEl) durEl.textContent = `${days} day${days > 1 ? 's' : ''}`;
    if (reasonEl) reasonEl.textContent = leave.reason || 'None provided';

    const currentUser = getCurrentUser();
    if (currentUser && currentUser.employeeId && currentUser.employeeId === leave.employeeId) {
        if (alertEl) {
            alertEl.textContent = 'Policy Violation: Reviewers cannot review or approve their own leave requests.';
            alertEl.classList.remove('d-none');
        }
        if (confirmBtn) confirmBtn.disabled = true;
    } else {
        if (confirmBtn) confirmBtn.disabled = false;
    }

    const modalEl = document.getElementById('reviewLeaveModal');
    const modal = new bootstrap.Modal(modalEl);
    modal.show();
}

async function handleReviewSubmit(e) {
    e.preventDefault();
    if (!reviewLeaveTargetId) return;

    const alertEl = document.getElementById('reviewLeaveAlert');
    const confirmBtn = document.getElementById('btnConfirmReview');
    if (alertEl) alertEl.classList.add('d-none');

    const statusRadio = document.querySelector('input[name="reviewDecision"]:checked');
    const status = statusRadio ? statusRadio.value : 'APPROVED';
    const commentsInput = document.getElementById('reviewComments');
    const reviewComments = commentsInput ? commentsInput.value.trim() : '';

    try {
        if (confirmBtn) confirmBtn.disabled = true;
        const res = await fetchWithAuth(`${LEAVE_API_BASE}/${reviewLeaveTargetId}/review`, {
            method: 'PUT',
            body: JSON.stringify({
                status,
                reviewComments
            })
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Failed to review leave request.');
        }

        const modalEl = document.getElementById('reviewLeaveModal');
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();

        showToast(`Leave request ${status.toLowerCase()} successfully.`, 'success');
        reviewLeaveTargetId = null;

        await loadAllLeaves();
        await loadMyLeaves();
    } catch (err) {
        if (alertEl) {
            alertEl.textContent = err.message;
            alertEl.classList.remove('d-none');
        } else {
            showToast(err.message, 'danger');
        }
    } finally {
        if (confirmBtn) confirmBtn.disabled = false;
    }
}

function openCancelLeaveModal(id) {
    cancelLeaveTargetId = id;
    const modalEl = document.getElementById('cancelLeaveModal');
    const modal = new bootstrap.Modal(modalEl);
    modal.show();
}

async function confirmCancelLeave() {
    if (!cancelLeaveTargetId) return;

    try {
        const res = await fetchWithAuth(`${LEAVE_API_BASE}/${cancelLeaveTargetId}/cancel`, {
            method: 'PUT'
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Failed to cancel leave request.');
        }

        const modalEl = document.getElementById('cancelLeaveModal');
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();

        showToast('Leave request cancelled successfully.', 'success');
        cancelLeaveTargetId = null;

        await loadMyLeaves();
        const user = getCurrentUser();
        if (user && (user.role === 'ADMIN' || user.role === 'HR')) {
            await loadAllLeaves();
        }
        updateLeaveDashboardMetrics();
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

/* ==========================================================================
   Phase 4: System Audit Trail Frontend Logic (ADMIN Only)
   ========================================================================== */

let allAuditLogsList = [];

async function initAuditLogsPage() {
    await loadAuditLogs();
}

async function loadAuditLogs() {
    const tbody = document.getElementById('auditLogsTableBody');
    if (!tbody) return;

    try {
        const res = await fetchWithAuth(AUDIT_API_BASE);
        if (res.status === 403) {
            showToast('Access Denied: Only administrators can view system audit logs.', 'danger');
            setTimeout(() => window.location.href = 'index.html', 1500);
            return;
        }

        if (!res.ok) {
            throw new Error('Failed to load audit logs.');
        }

        allAuditLogsList = await res.json();
        renderAuditLogsTable(allAuditLogsList);
        updateAuditLogStats(allAuditLogsList);
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-danger"><i class="bi bi-exclamation-triangle me-1"></i> ${err.message}</td></tr>`;
    }
}

function renderAuditLogsTable(logs) {
    const tbody = document.getElementById('auditLogsTableBody');
    if (!tbody) return;

    if (!logs || logs.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-5 text-muted">
            <i class="bi bi-journal-x fs-2 d-block mb-2 text-secondary"></i>
            No audit records matching the specified criteria.
        </td></tr>`;
        return;
    }

    tbody.innerHTML = logs.map(log => {
        const actionBadge = getAuditActionBadge(log.action);
        const entityBadge = getAuditEntityBadge(log.entityType);
        const formattedTime = formatAuditTimestamp(log.timestamp);

        return `
            <tr>
                <td class="ps-3 fw-bold text-muted">#${log.id}</td>
                <td>
                    <div class="small fw-semibold text-dark">${formattedTime.date}</div>
                    <div class="small text-muted">${formattedTime.time}</div>
                </td>
                <td>
                    <span class="badge bg-light text-dark border"><i class="bi bi-person-fill text-secondary me-1"></i>${escapeHtml(log.actor || 'SYSTEM')}</span>
                </td>
                <td>${actionBadge}</td>
                <td>${entityBadge}</td>
                <td class="text-muted fw-semibold">${log.entityId ? `#${log.entityId}` : '-'}</td>
                <td><span class="small font-monospace text-wrap">${escapeHtml(log.details || '')}</span></td>
            </tr>
        `;
    }).join('');
}

function filterAuditLogs() {
    const entityFilter = document.getElementById('auditEntityFilter');
    const actionFilter = document.getElementById('auditActionFilter');
    const searchInput = document.getElementById('auditSearchInput');

    const entity = entityFilter ? entityFilter.value : '';
    const action = actionFilter ? actionFilter.value : '';
    const query = searchInput ? searchInput.value.toLowerCase().trim() : '';

    let filtered = allAuditLogsList;

    if (entity) {
        filtered = filtered.filter(l => l.entityType === entity);
    }

    if (action) {
        filtered = filtered.filter(l => l.action === action);
    }

    if (query) {
        filtered = filtered.filter(l => {
            const actor = (l.actor || '').toLowerCase();
            const details = (l.details || '').toLowerCase();
            const id = String(l.id || '');
            const entityId = String(l.entityId || '');
            return actor.includes(query) || details.includes(query) || id.includes(query) || entityId.includes(query);
        });
    }

    renderAuditLogsTable(filtered);
}

function resetAuditFilters() {
    const entityFilter = document.getElementById('auditEntityFilter');
    const actionFilter = document.getElementById('auditActionFilter');
    const searchInput = document.getElementById('auditSearchInput');

    if (entityFilter) entityFilter.value = '';
    if (actionFilter) actionFilter.value = '';
    if (searchInput) searchInput.value = '';

    renderAuditLogsTable(allAuditLogsList);
}

function updateAuditLogStats(logs) {
    const totalEl = document.getElementById('statAuditTotal');
    const empEl = document.getElementById('statAuditEmployee');
    const deptEl = document.getElementById('statAuditDepartment');
    const leaveEl = document.getElementById('statAuditLeave');

    if (!totalEl) return;

    totalEl.textContent = logs.length;
    if (empEl) empEl.textContent = logs.filter(l => l.entityType === 'EMPLOYEE').length;
    if (deptEl) deptEl.textContent = logs.filter(l => l.entityType === 'DEPARTMENT').length;
    if (leaveEl) leaveEl.textContent = logs.filter(l => l.entityType === 'LEAVE').length;
}

function getAuditActionBadge(action) {
    if (!action) return '<span class="badge bg-secondary">UNKNOWN</span>';

    if (action.includes('CREATED')) {
        return `<span class="badge bg-success-subtle text-success border border-success-subtle"><i class="bi bi-plus-circle me-1"></i>${action}</span>`;
    }
    if (action.includes('UPDATED')) {
        return `<span class="badge bg-primary-subtle text-primary border border-primary-subtle"><i class="bi bi-pencil me-1"></i>${action}</span>`;
    }
    if (action.includes('TERMINATED') || action.includes('DELETED')) {
        return `<span class="badge bg-danger-subtle text-danger border border-danger-subtle"><i class="bi bi-trash me-1"></i>${action}</span>`;
    }
    if (action === 'LEAVE_APPROVED') {
        return `<span class="badge bg-success-subtle text-success border border-success-subtle"><i class="bi bi-check-circle me-1"></i>${action}</span>`;
    }
    if (action === 'LEAVE_REJECTED') {
        return `<span class="badge bg-danger-subtle text-danger border border-danger-subtle"><i class="bi bi-x-circle me-1"></i>${action}</span>`;
    }
    if (action === 'LEAVE_CANCELLED') {
        return `<span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle"><i class="bi bi-slash-circle me-1"></i>${action}</span>`;
    }
    if (action === 'LEAVE_SUBMITTED') {
        return `<span class="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle"><i class="bi bi-clock me-1"></i>${action}</span>`;
    }

    return `<span class="badge bg-secondary">${action}</span>`;
}

function getAuditEntityBadge(entityType) {
    switch (entityType) {
        case 'EMPLOYEE':
            return '<span class="badge bg-info text-dark">Employee</span>';
        case 'DEPARTMENT':
            return '<span class="badge bg-primary">Department</span>';
        case 'LEAVE':
            return '<span class="badge bg-warning text-dark">Leave</span>';
        default:
            return `<span class="badge bg-light text-dark">${escapeHtml(entityType || 'SYSTEM')}</span>`;
    }
}

function formatAuditTimestamp(isoString) {
    if (!isoString) return { date: 'N/A', time: '' };
    const d = new Date(isoString);
    return {
        date: d.toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' }),
        time: d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true })
    };
}


