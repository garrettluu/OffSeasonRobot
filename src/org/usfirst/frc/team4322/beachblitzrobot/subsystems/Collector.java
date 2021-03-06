package org.usfirst.frc.team4322.beachblitzrobot.subsystems;

import org.usfirst.frc.team4322.beachblitzrobot.Robot;
import org.usfirst.frc.team4322.beachblitzrobot.RobotMap;
import org.usfirst.frc.team4322.beachblitzrobot.commands.Collector_Raise;
import org.usfirst.frc.team4322.beachblitzrobot.commands.Collector_Stop;
import org.usfirst.frc.team4322.beachblitzrobot.commands.Feeder_StopFeeder;

import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.Talon;
import edu.wpi.first.wpilibj.DoubleSolenoid.Value;
import edu.wpi.first.wpilibj.InterruptHandlerFunction;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Subsystem;

/**
 *
 */
public class Collector extends Subsystem {
    
    private DoubleSolenoid collectorPistons;
    private Talon collectorTalon;
    private DigitalInput ballSwitch;
    public boolean intenabled = false; 
    
    private static class BallInterruptHandler extends InterruptHandlerFunction<Object>
    {

        @Override
        public void interruptFired(int interruptAssertedMask, Object param)
        {
            Robot.collector.ballSwitch.requestInterrupts(this);
            Robot.collector.ballSwitch.enableInterrupts();
            Robot.feeder.set(0);
        }
        
    }
    public Collector()
    {
        collectorPistons = new DoubleSolenoid(RobotMap.COLLECTOR_ARM_DOUBLESOLENOID_LEFT_PORT,
                                              RobotMap.COLLECTOR_ARM_DOUBLESOLENOID_RIGHT_PORT);
        collectorTalon = new Talon(RobotMap.COLLECTOR_ARM_TALONSR_ID);
        collectorPistons.set(Value.kReverse);
        ballSwitch = new DigitalInput(RobotMap.COLLECTOR_BALL_SWITCH_DIO_PORT);
        ballSwitch.requestInterrupts(new BallInterruptHandler());
        ballSwitch.enableInterrupts();

    }


    /*@Override
    protected Command getDefaultCommand()
    {
        return new Collector_Raise();
    }*/
    
    public boolean ballSwitchTriggered()
    {
        return ballSwitch.get();
    }
    
    public void extendArm()
    {
        collectorPistons.set(Value.kForward);

    }
    
    public void retractArm()
    {
        collectorPistons.set(Value.kReverse);
    }
    
    public void collectorStop()
    {
        collectorTalon.set(0);
    }
    
    public void collectorForward()
    {
        collectorTalon.set(-1);
    }
    
    public void collectorBackward()
    {
        collectorTalon.set(1);
    }
    
    public void initDefaultCommand() {
        // Set the default command for a subsystem here.
        //setDefaultCommand(new MySpecialCommand());
    }
}

