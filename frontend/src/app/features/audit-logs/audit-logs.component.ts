import { Component, OnInit, signal } from '@angular/core';
import { CommonModule }              from '@angular/common';
import { FormsModule }               from '@angular/forms';
import { AuditService }              from '../../core/services/audit.service';
import { AuditLog, AuditPage }       from '../../core/models/audit.model';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit-logs.component.html',
  styleUrls: ['./audit-logs.component.scss']
})
export class AuditLogsComponent implements OnInit {

  // ── state ────────────────────────────────────────────────────
  logs    = signal<AuditLog[]>([]);
  loading = signal(false);
  error   = signal<string | null>(null);

  total      = 0;
  totalPages = 0;
  page       = 0;
  pageSize   = 20;

  // ── filters ──────────────────────────────────────────────────
  filterUsername = '';
  filterAction   = '';
  filterResource = '';
  filterStatus   = '';
  filterFrom     = '';
  filterTo       = '';

  // ── dropdown options ─────────────────────────────────────────
  readonly actions = [
    'USER_LOGIN','USER_LOGOUT','USER_REGISTER','USER_REFRESH_TOKEN',
    'USER_DELETED','USER_UPDATED','USER_ROLE_CHANGED',
    'PIPELINE_CREATED','PIPELINE_UPDATED','PIPELINE_DELETED',
    'PIPELINE_TRIGGERED','PIPELINE_ABORTED',
    'PROJECT_CREATED','PROJECT_UPDATED','PROJECT_DELETED',
    'SECURITY_SCAN_TRIGGERED','SECURITY_SCAN_COMPLETED','SECURITY_SCAN_BLOCKED',
    'SYSTEM_EVENT'
  ];

  readonly resources = [
    'USER','PIPELINE','PROJECT','SECURITY_SCAN'
  ];

  constructor(private auditSvc: AuditService) {}

  ngOnInit(): void { this.fetch(); }

  fetch(): void {
    this.loading.set(true);
    this.error.set(null);

    this.auditSvc.getLogs({
      username:  this.filterUsername  || undefined,
      action:    this.filterAction    || undefined,
      resource:  this.filterResource  || undefined,
      status:    this.filterStatus    || undefined,
      from:      this.filterFrom      ? this.filterFrom + ':00' : undefined,
      to:        this.filterTo        ? this.filterTo   + ':00' : undefined,
      page:      this.page,
      size:      this.pageSize,
    }).subscribe({
      next: (page: AuditPage) => {
        this.logs.set(page.content);
        this.total      = page.totalElements;
        this.totalPages = page.totalPages;
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load audit logs: ' + (err.message ?? 'Unknown error'));
        this.loading.set(false);
      }
    });
  }

  applyFilters(): void {
    this.page = 0;
    this.fetch();
  }

  resetFilters(): void {
    this.filterUsername = '';
    this.filterAction   = '';
    this.filterResource = '';
    this.filterStatus   = '';
    this.filterFrom     = '';
    this.filterTo       = '';
    this.page = 0;
    this.fetch();
  }

  prevPage(): void { if (this.page > 0) { this.page--; this.fetch(); } }
  nextPage(): void { if (this.page < this.totalPages - 1) { this.page++; this.fetch(); } }

  actionBadgeClass(action: string): string {
    if (action.startsWith('USER_'))     return 'badge-user';
    if (action.startsWith('PIPELINE_')) return 'badge-pipeline';
    if (action.startsWith('PROJECT_'))  return 'badge-project';
    if (action.startsWith('SECURITY_')) return 'badge-security';
    return 'badge-system';
  }

  formatTs(ts: string): string {
    return new Date(ts).toLocaleString('fr-TN', {
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit', second: '2-digit'
    });
  }
}
