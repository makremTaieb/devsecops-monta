// ── notifications.component.ts ─────────────────────────────────
import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../core/services/notification.service';
import { NotificationItem, EventType } from '../../core/models/notification.model';

type FilterType = 'ALL' | 'EMAIL' | 'SLACK' | 'SENT' | 'FAILED' | 'PENDING';

@Component({
  selector:    'app-notifications',
  standalone:  true,
  imports:     [CommonModule],
  templateUrl: './notifications.component.html',
  styleUrls:   ['./notifications.component.scss'],
})
export class NotificationsComponent implements OnInit {

  all      = signal<NotificationItem[]>([]);
  filter   = signal<FilterType>('ALL');
  loading  = signal(true);

  readonly filtered = computed(() => {
    const f = this.filter();
    const data = this.all();
    if (f === 'ALL')     return data;
    if (f === 'EMAIL')   return data.filter(n => n.channel === 'EMAIL');
    if (f === 'SLACK')   return data.filter(n => n.channel === 'SLACK');
    if (f === 'SENT')    return data.filter(n => n.status  === 'SENT');
    if (f === 'FAILED')  return data.filter(n => n.status  === 'FAILED');
    if (f === 'PENDING') return data.filter(n => n.status  === 'PENDING');
    return data;
  });

  readonly counts = computed(() => ({
    all:     this.all().length,
    failed:  this.all().filter(n => n.status === 'FAILED').length,
    pending: this.all().filter(n => n.status === 'PENDING').length,
  }));

  readonly filters: { key: FilterType; label: string }[] = [
    { key: 'ALL',     label: 'Toutes' },
    { key: 'EMAIL',   label: 'Email' },
    { key: 'SLACK',   label: 'Slack' },
    { key: 'SENT',    label: 'Envoyées' },
    { key: 'PENDING', label: 'En attente' },
    { key: 'FAILED',  label: 'Échouées' },
  ];

  constructor(private svc: NotificationService) {}

  ngOnInit(): void { this.load(); }

  // ✅ Extracted load() method — safe for refresh button
  load(): void {
    this.loading.set(true);
    this.svc.getAll().subscribe({
      next:  n => { this.all.set(n); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  setFilter(f: FilterType): void { this.filter.set(f); }

  iconFor(type: EventType): string {
    const map: Record<string, string> = {
      PIPELINE_SUCCESS: '✓',
      PIPELINE_FAILED:  '✕',
      SECURITY_BLOCKED: '🛡',
      SECURITY_WARNING: '⚠',
    };
    return map[type] ?? '•';
  }

  iconColorClass(type: EventType): string {
    if (type === 'PIPELINE_SUCCESS')                                return 'green';
    if (type === 'PIPELINE_FAILED' || type === 'SECURITY_BLOCKED') return 'red';
    return 'orange';
  }
}
