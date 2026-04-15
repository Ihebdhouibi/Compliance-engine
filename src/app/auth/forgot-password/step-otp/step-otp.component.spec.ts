import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StepOtpComponent } from './step-otp.component';

describe('StepOtpComponent', () => {
  let component: StepOtpComponent;
  let fixture: ComponentFixture<StepOtpComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StepOtpComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StepOtpComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
