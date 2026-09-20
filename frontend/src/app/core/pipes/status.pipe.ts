import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'statusCount', standalone: true })
export class StatusCountPipe implements PipeTransform {
  transform(items: any[], status: string): number {
    return items.filter(i => i.status === status).length;
  }
}

@Pipe({ name: 'statusFilter', standalone: true })
export class StatusFilterPipe implements PipeTransform {
  transform(items: any[], statuses: string[]): any[] {
    return items.filter(i => statuses.includes(i.status));
  }
}
