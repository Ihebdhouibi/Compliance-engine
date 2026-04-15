import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StepResetComponent } from './step-reset.component';

describe('StepResetComponent', () => {
  let component: StepResetComponent;
  let fixture: ComponentFixture<StepResetComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StepResetComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StepResetComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
