package uk.pigpioj;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PigpioJNI implements PigpioInterface {
	private static final Logger LOGGER = Logger.getLogger(PigpioJNI.class.getName());
	private static final String PIGPIO_CLOCK_CFG_MICROS = "PIGPIO_CLOCK_CFG_MICROS";
	private static final String PIGPIO_CLOCK_CFG_PERIPHERAL = "PIGPIO_CLOCK_CFG_PERIPHERAL";
	private static final String LIB_NAME = "pigpioj";
	private static AtomicBoolean loaded = new AtomicBoolean();

	public static synchronized int initialise() {
		if (!loaded.get()) {
			if (!LibraryUtil.loadLibrary(LIB_NAME, PigpioJNI.class)) {
				throw new RuntimeException("Error loading native library '" + LIB_NAME + "'");
			}

			loaded.set(true);

			String clock_cfg_micros_s = PigpioJ.getProperty(PIGPIO_CLOCK_CFG_MICROS);
			int clock_cfg_micros = -1;
			if (clock_cfg_micros_s != null) {
				try {
					clock_cfg_micros = Integer.parseInt(clock_cfg_micros_s);
				} catch (NumberFormatException e) {
					LOGGER.log(Level.WARNING,
							"Invalid " + PIGPIO_CLOCK_CFG_MICROS + " value '" + clock_cfg_micros_s + "'");
				}
			}

			String clock_cfg_peripheral_s = PigpioJ.getProperty(PIGPIO_CLOCK_CFG_PERIPHERAL);
			int clock_cfg_peripheral = -1;
			if (clock_cfg_peripheral_s != null) {
				try {
					clock_cfg_peripheral = Integer.parseInt(clock_cfg_peripheral_s);
				} catch (NumberFormatException e) {
					LOGGER.log(Level.WARNING,
							"Invalid " + PIGPIO_CLOCK_CFG_PERIPHERAL + " value '" + clock_cfg_peripheral_s + "'");
				}
			}

			return PigpioGpio.initialise(clock_cfg_micros, clock_cfg_peripheral);
		}

		return loaded.get() ? PigpioConstants.SUCCESS : PigpioConstants.ERROR;
	}

	public PigpioJNI() {
		int rc = initialise();
		if (rc < 0) {
			throw new RuntimeException("Error initialising pigpio (must be run as root!): " + rc);
		}
	}

	@Override
	public void close() {
		PigpioGpio.terminate();
	}

	@Override
	public int enableListener(int gpio, int edge, PigpioCallback callback) {
		// Note that setISRFunc uses the underlying Linux sysfs GPIO interface for
		// interrupts.
		// The glitch filter has no affect - use setAlertFunc instead
		return PigpioGpio.setISRFunc(gpio, edge, -1, callback);
	}

	@Override
	public int disableListener(int gpio) {
		return PigpioGpio.setISRFunc(gpio, PigpioConstants.EITHER_EDGE, -1, null);
	}

	@Override
	public int getVersion() {
		return PigpioGpio.getVersion();
	}

	@Override
	public int getHardwareRevision() {
		return PigpioGpio.getHardwareRevision();
	}

	@Override
	public int getMode(int gpio) {
		return PigpioGpio.getMode(gpio);
	}

	@Override
	public int setMode(int gpio, int mode) {
		return PigpioGpio.setMode(gpio, mode);
	}

	@Override
	public int setPullUpDown(int gpio, int pud) {
		return PigpioGpio.setPullUpDown(gpio, pud);
	}

	@Override
	public int read(int gpio) {
		return PigpioGpio.read(gpio);
	}

	@Override
	public int write(int gpio, boolean level) {
		return PigpioGpio.write(gpio, level);
	}

	@Override
	public int getPWMDutyCycle(int gpio) {
		return PigpioGpio.getPWMDutyCycle(gpio);
	}

	@Override
	public int setPWMDutyCycle(int gpio, int dutyCycle) {
		return PigpioGpio.setPWMDutyCycle(gpio, dutyCycle);
	}

	@Override
	public int getPWMRange(int gpio) {
		return PigpioGpio.getPWMRange(gpio);
	}

	@Override
	public int setPWMRange(int gpio, int range) {
		return PigpioGpio.setPWMDutyCycle(gpio, range);
	}

	@Override
	public int getPWMRealRange(int gpio) {
		return PigpioGpio.getPWMRealRange(gpio);
	}

	@Override
	public int getPWMFrequency(int gpio) {
		return PigpioGpio.getPWMFrequency(gpio);
	}

	@Override
	public int setPWMFrequency(int gpio, int frequency) {
		return PigpioGpio.setPWMFrequency(gpio, frequency);
	}

	@Override
	public int getServoPulseWidth(int gpio) {
		return PigpioGpio.getServoPulseWidth(gpio);
	}

	@Override
	public int setServoPulseWidth(int gpio, int pulseWidth) {
		return PigpioGpio.setServoPulseWidth(gpio, pulseWidth);
	}

	@Override
	public int noiseFilter(int gpio, int steadyMs, int activeMs) {
		return PigpioGpio.noiseFilter(gpio, steadyMs, activeMs);
	}

	@Override
	public int glitchFilter(int gpio, int steadyMs) {
		return PigpioGpio.glitchFilter(gpio, steadyMs);
	}

	@Override
	public int hardwareClock(int gpio, int clockFreq) {
		return PigpioGpio.hardwareClock(gpio, clockFreq);
	}

	@Override
	public int hardwarePwm(int gpio, int pwmFreq, int pwmDuty) {
		return PigpioGpio.hardwarePwm(gpio, pwmFreq, pwmDuty);
	}

	@Override
	public int i2cOpen(int i2cBus, int i2cAddr, int i2cFlags) {
		return PigpioI2C.i2cOpen(i2cBus, i2cAddr, i2cFlags);
	}

	@Override
	public int i2cClose(int handle) {
		return PigpioI2C.i2cClose(handle);
	}

	@Override
	public int i2cWriteQuick(int handle, int bit) {
		return PigpioI2C.i2cWriteQuick(handle, bit);
	}

	@Override
	public int i2cReadByte(int handle) {
		return PigpioI2C.i2cReadByte(handle);
	}

	@Override
	public int i2cWriteByte(int handle, int bVal) {
		return PigpioI2C.i2cWriteByte(handle, bVal);
	}

	@Override
	public int i2cReadByteData(int handle, int i2cReg) {
		return PigpioI2C.i2cReadByteData(handle, i2cReg);
	}

	@Override
	public int i2cWriteByteData(int handle, int i2cReg, int bVal) {
		return PigpioI2C.i2cWriteByteData(handle, i2cReg, bVal);
	}

	@Override
	public int i2cReadWordData(int handle, int i2cReg) {
		return PigpioI2C.i2cReadWordData(handle, i2cReg);
	}

	@Override
	public int i2cWriteWordData(int handle, int i2cReg, int wVal) {
		return PigpioI2C.i2cWriteWordData(handle, i2cReg, wVal);
	}

	@Override
	public int i2cProcessCall(int handle, int i2cReg, int wVal) {
		return PigpioI2C.i2cProcessCall(handle, i2cReg, wVal);
	}

	@Override
	public int i2cReadBlockData(int handle, int i2cReg, byte[] buf) {
		return PigpioI2C.i2cReadBlockData(handle, i2cReg, buf);
	}

	@Override
	public int i2cWriteBlockData(int handle, int i2cReg, byte[] buf, int count) {
		return PigpioI2C.i2cWriteBlockData(handle, i2cReg, buf, count);
	}

	@Override
	public int i2cBlockProcessCall(int handle, int i2cReg, byte[] buf, int count) {
		return PigpioI2C.i2cBlockProcessCall(handle, i2cReg, buf, count);
	}

	@Override
	public int i2cReadI2CBlockData(int handle, int i2cReg, byte[] buf, int count) {
		return PigpioI2C.i2cReadI2CBlockData(handle, i2cReg, buf, count);
	}

	@Override
	public int i2cWriteI2CBlockData(int handle, int i2cReg, byte[] buf, int count) {
		return PigpioI2C.i2cWriteI2CBlockData(handle, i2cReg, buf, count);
	}

	@Override
	public int i2cReadDevice(int handle, byte[] buffer, int count) {
		return PigpioI2C.i2cReadDevice(handle, buffer, count);
	}

	@Override
	public int i2cWriteDevice(int handle, byte[] buffer, int count) {
		return PigpioI2C.i2cWriteDevice(handle, buffer, count);
	}

	@Override
	public int i2cSegments(int handle, PiI2CMessage[] segs, byte[] buffer) {
		return PigpioI2C.i2cSegments(handle, segs, buffer);
	}

	@Override
	public int spiOpen(int spiChan, int baud, int spiFlags) {
		return PigpioSPI.spiOpen(spiChan, baud, spiFlags);
	}

	@Override
	public int spiClose(int handle) {
		return PigpioSPI.spiClose(handle);
	}

	@Override
	public int spiRead(int handle, byte[] buf, int count) {
		return PigpioSPI.spiRead(handle, buf, count);
	}

	@Override
	public int spiWrite(int handle, byte[] buf, int offset, int length) {
		return PigpioSPI.spiWrite(handle, buf, offset, length);
	}

	@Override
	public int spiXfer(int handle, byte[] txBuf, byte[] rxBuf, int count) {
		return PigpioSPI.spiXfer(handle, txBuf, rxBuf, count);
	}

	@Override
	public int gpioWaveClear() {
		return PigpioWaveform.gpioWaveClear();
	}

	@Override
	public int gpioWaveAddNew() {
		return PigpioWaveform.gpioWaveAddNew();
	}

	@Override
	public int gpioWaveAddGeneric(GpioPulse[] pulses) {
		return PigpioWaveform.gpioWaveAddGeneric(pulses);
	}

	@Override
	public int gpioWaveAddSerial(int userGpio, int baud, int dataBits, int stopBits, int offset, byte[] str) {
		return PigpioWaveform.gpioWaveAddSerial(userGpio, baud, dataBits, stopBits, offset, str);
	}

	@Override
	public int gpioWaveCreate() {
		return PigpioWaveform.gpioWaveCreate();
	}

	@Override
	public int gpioWaveCreatePad(int pctCB, int pctBOOL, int pctTOOL) {
		return PigpioWaveform.gpioWaveCreatePad(pctCB, pctBOOL, pctTOOL);
	}

	@Override
	public int gpioWaveDelete(int waveId) {
		return PigpioWaveform.gpioWaveDelete(waveId);
	}

	@Override
	public int gpioWaveTxSend(int waveId, int waveMode) {
		return PigpioWaveform.gpioWaveTxSend(waveId, waveMode);
	}

	@Override
	public int gpioWaveChain(byte[] buf) {
		return PigpioWaveform.gpioWaveChain(buf);
	}

	@Override
	public int gpioWaveTxAt() {
		return PigpioWaveform.gpioWaveTxAt();
	}

	@Override
	public int gpioWaveTxBusy() {
		return PigpioWaveform.gpioWaveTxBusy();
	}

	@Override
	public int gpioWaveTxStop() {
		return PigpioWaveform.gpioWaveTxStop();
	}

	@Override
	public int gpioWaveGetMicros() {
		return PigpioWaveform.gpioWaveGetMicros();
	}

	@Override
	public int gpioWaveGetHighMicros() {
		return PigpioWaveform.gpioWaveGetHighMicros();
	}

	@Override
	public int gpioWaveGetMaxMicros() {
		return PigpioWaveform.gpioWaveGetMaxMicros();
	}

	@Override
	public int gpioWaveGetPulses() {
		return PigpioWaveform.gpioWaveGetPulses();
	}

	@Override
	public int gpioWaveGetHighPulses() {
		return PigpioWaveform.gpioWaveGetHighPulses();
	}

	@Override
	public int gpioWaveGetMaxPulses() {
		return PigpioWaveform.gpioWaveGetMaxPulses();
	}

	@Override
	public int gpioWaveGetCbs() {
		return PigpioWaveform.gpioWaveGetCbs();
	}

	@Override
	public int gpioWaveGetHighCbs() {
		return PigpioWaveform.gpioWaveGetHighCbs();
	}

	@Override
	public int gpioWaveGetMaxCbs() {
		return PigpioWaveform.gpioWaveGetMaxCbs();
	}

	@Override
	public int serOpen(String sertty, int baud, int serFlags) {
		return PigpioSerial.serOpen(sertty, baud, serFlags);
	}

	@Override
	public int serClose(int handle) {
		return PigpioSerial.serClose(handle);
	}

	@Override
	public int serWriteByte(int handle, int bVal) {
		return PigpioSerial.serWriteByte(handle, bVal);
	}

	@Override
	public int serReadByte(int handle) {
		return PigpioSerial.serReadByte(handle);
	}

	@Override
	public int serWrite(int handle, byte[] buf, int count) {
		return PigpioSerial.serWrite(handle, buf, count);
	}

	@Override
	public int serRead(int handle, byte[] buf, int count) {
		return PigpioSerial.serRead(handle, buf, count);
	}

	@Override
	public int serDataAvailable(int handle) {
		return PigpioSerial.serDataAvailable(handle);
	}
}
