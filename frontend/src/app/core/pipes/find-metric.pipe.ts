import { Pipe, PipeTransform } from '@angular/core';
import { MetricPoint } from '../services/prometheus.service';

/** Finds a MetricPoint by service name, returns the value (or default) */
@Pipe({ name: 'findMetric', standalone: true })
export class FindMetricPipe implements PipeTransform {
  transform(points: MetricPoint[], serviceName: string, defaultVal = 0): number {
    const found = points?.find(p => p.service === serviceName);
    return found ? found.value : defaultVal;
  }
}
