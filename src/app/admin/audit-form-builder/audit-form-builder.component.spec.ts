import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AuditFormBuilderComponent } from './audit-form-builder.component';

describe('AuditFormBuilderComponent', () => {
  let component: AuditFormBuilderComponent;
  let fixture: ComponentFixture<AuditFormBuilderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuditFormBuilderComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AuditFormBuilderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
