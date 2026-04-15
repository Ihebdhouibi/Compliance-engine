import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AuditsComponent } from './audits.component';

describe('AuditsComponent', () => {
  let component: AuditsComponent;
  let fixture: ComponentFixture<AuditsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuditsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AuditsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
