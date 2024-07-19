package uk.pigpioj;

public interface PigpioConstants {
	int ERROR = -1;
	int SUCCESS = 0;

	int MODE_PI_INPUT = 0;
	int MODE_PI_OUTPUT = 1;
	int MODE_PI_ALT0 = 4;
	int MODE_PI_ALT1 = 5;
	int MODE_PI_ALT2 = 6;
	int MODE_PI_ALT3 = 7;
	int MODE_PI_ALT4 = 3;
	int MODE_PI_ALT5 = 2;

	int NO_EDGE = -1;
	int RISING_EDGE = 0;
	int FALLING_EDGE = 1;
	int EITHER_EDGE = 2;

	int PI_PUD_OFF = 0;
	int PI_PUD_DOWN = 1;
	int PI_PUD_UP = 2;

	int PI_OFF = 0;
	int PI_ON = 1;

	// Error codes
	int PI_INIT_FAILED = -1; // gpioInitialise failed
	int PI_BAD_USER_GPIO = -2; // GPIO not 0-31
	int PI_BAD_GPIO = -3; // GPIO not 0-53
	int PI_BAD_MODE = -4; // mode not 0-7
	int PI_BAD_LEVEL = -5; // level not 0-1
	int PI_BAD_PUD = -6; // pud not 0-2
	int PI_BAD_PULSEWIDTH = -7; // pulsewidth not 0 or 500-2500
	int PI_BAD_DUTYCYCLE = -8; // dutycycle outside set range
	int PI_BAD_TIMER = -9; // timer not 0-9
	int PI_BAD_MS = -10; // ms not 10-60000
	int PI_BAD_TIMETYPE = -11; // timetype not 0-1
	int PI_BAD_SECONDS = -12; // seconds < 0
	int PI_BAD_MICROS = -13; // micros not 0-999999
	int PI_TIMER_FAILED = -14; // gpioSetTimerFunc failed
	int PI_BAD_WDOG_TIMEOUT = -15; // timeout not 0-60000
	int PI_NO_ALERT_FUNC = -16; // DEPRECATED
	int PI_BAD_CLK_PERIPH = -17; // clock peripheral not 0-1
	int PI_BAD_CLK_SOURCE = -18; // DEPRECATED
	int PI_BAD_CLK_MICROS = -19; // clock micros not 1, 2, 4, 5, 8, or 10
	int PI_BAD_BUF_MILLIS = -20; // buf millis not 100-10000
	int PI_BAD_DUTYRANGE = -21; // dutycycle range not 25-40000
	int PI_BAD_DUTY_RANGE = -21; // DEPRECATED (use PI_BAD_DUTYRANGE)
	int PI_BAD_SIGNUM = -22; // signum not 0-63
	int PI_BAD_PATHNAME = -23; // can't open pathname
	int PI_NO_HANDLE = -24; // no handle available
	int PI_BAD_HANDLE = -25; // unknown handle
	int PI_BAD_IF_FLAGS = -26; // ifFlags > 3
	int PI_BAD_CHANNEL = -27; // DMA channel not 0-14
	int PI_BAD_PRIM_CHANNEL = -27; // DMA primary channel not 0-14
	int PI_BAD_SOCKET_PORT = -28; // socket port not 1024-32000
	int PI_BAD_FIFO_COMMAND = -29; // unrecognized fifo command
	int PI_BAD_SECO_CHANNEL = -30; // DMA secondary channel not 0-6
	int PI_NOT_INITIALISED = -31; // function called before gpioInitialise
	int PI_INITIALISED = -32; // function called after gpioInitialise
	int PI_BAD_WAVE_MODE = -33; // waveform mode not 0-3
	int PI_BAD_CFG_INTERNAL = -34; // bad parameter in gpioCfgInternals call
	int PI_BAD_WAVE_BAUD = -35; // baud rate not 50-250K(RX)/50-1M(TX)
	int PI_TOO_MANY_PULSES = -36; // waveform has too many pulses
	int PI_TOO_MANY_CHARS = -37; // waveform has too many chars
	int PI_NOT_SERIAL_GPIO = -38; // no bit bang serial read on GPIO
	int PI_BAD_SERIAL_STRUC = -39; // bad (null) serial structure parameter
	int PI_BAD_SERIAL_BUF = -40; // bad (null) serial buf parameter
	int PI_NOT_PERMITTED = -41; // GPIO operation not permitted
	int PI_SOME_PERMITTED = -42; // one or more GPIO not permitted
	int PI_BAD_WVSC_COMMND = -43; // bad WVSC subcommand
	int PI_BAD_WVSM_COMMND = -44; // bad WVSM subcommand
	int PI_BAD_WVSP_COMMND = -45; // bad WVSP subcommand
	int PI_BAD_PULSELEN = -46; // trigger pulse length not 1-100
	int PI_BAD_SCRIPT = -47; // invalid script
	int PI_BAD_SCRIPT_ID = -48; // unknown script id
	int PI_BAD_SER_OFFSET = -49; // add serial data offset > 30 minutes
	int PI_GPIO_IN_USE = -50; // GPIO already in use
	int PI_BAD_SERIAL_COUNT = -51; // must read at least a byte at a time
	int PI_BAD_PARAM_NUM = -52; // script parameter id not 0-9
	int PI_DUP_TAG = -53; // script has duplicate tag
	int PI_TOO_MANY_TAGS = -54; // script has too many tags
	int PI_BAD_SCRIPT_CMD = -55; // illegal script command
	int PI_BAD_VAR_NUM = -56; // script variable id not 0-149
	int PI_NO_SCRIPT_ROOM = -57; // no more room for scripts
	int PI_NO_MEMORY = -58; // can't allocate temporary memory
	int PI_SOCK_READ_FAILED = -59; // socket read failed
	int PI_SOCK_WRIT_FAILED = -60; // socket write failed
	int PI_TOO_MANY_PARAM = -61; // too many script parameters (> 10)
	int PI_NOT_HALTED = -62; // DEPRECATED
	int PI_SCRIPT_NOT_READY = -62; // script initialising
	int PI_BAD_TAG = -63; // script has unresolved tag
	int PI_BAD_MICS_DELAY = -64; // bad MICS delay (too large)
	int PI_BAD_MILS_DELAY = -65; // bad MILS delay (too large)
	int PI_BAD_WAVE_ID = -66; // non existent wave id
	int PI_TOO_MANY_CBS = -67; // No more CBs for waveform
	int PI_TOO_MANY_OOL = -68; // No more OOL for waveform
	int PI_EMPTY_WAVEFORM = -69; // attempt to create an empty waveform
	int PI_NO_WAVEFORM_ID = -70; // no more waveforms
	int PI_I2C_OPEN_FAILED = -71; // can't open I2C device
	int PI_SER_OPEN_FAILED = -72; // can't open serial device
	int PI_SPI_OPEN_FAILED = -73; // can't open SPI device
	int PI_BAD_I2C_BUS = -74; // bad I2C bus
	int PI_BAD_I2C_ADDR = -75; // bad I2C address
	int PI_BAD_SPI_CHANNEL = -76; // bad SPI channel
	int PI_BAD_FLAGS = -77; // bad i2c/spi/ser open flags
	int PI_BAD_SPI_SPEED = -78; // bad SPI speed
	int PI_BAD_SER_DEVICE = -79; // bad serial device name
	int PI_BAD_SER_SPEED = -80; // bad serial baud rate
	int PI_BAD_PARAM = -81; // bad i2c/spi/ser parameter
	int PI_I2C_WRITE_FAILED = -82; // i2c write failed
	int PI_I2C_READ_FAILED = -83; // i2c read failed
	int PI_BAD_SPI_COUNT = -84; // bad SPI count
	int PI_SER_WRITE_FAILED = -85; // ser write failed
	int PI_SER_READ_FAILED = -86; // ser read failed
	int PI_SER_READ_NO_DATA = -87; // ser read no data available
	int PI_UNKNOWN_COMMAND = -88; // unknown command
	int PI_SPI_XFER_FAILED = -89; // spi xfer/read/write failed
	int PI_BAD_POINTER = -90; // bad (NULL) pointer
	int PI_NO_AUX_SPI = -91; // no auxiliary SPI on Pi A or B
	int PI_NOT_PWM_GPIO = -92; // GPIO is not in use for PWM
	int PI_NOT_SERVO_GPIO = -93; // GPIO is not in use for servo pulses
	int PI_NOT_HCLK_GPIO = -94; // GPIO has no hardware clock
	int PI_NOT_HPWM_GPIO = -95; // GPIO has no hardware PWM
	int PI_BAD_HPWM_FREQ = -96; // hardware PWM frequency not 1-125M
	int PI_BAD_HPWM_DUTY = -97; // hardware PWM dutycycle not 0-1M
	int PI_BAD_HCLK_FREQ = -98; // hardware clock frequency not 4689-250M
	int PI_BAD_HCLK_PASS = -99; // need password to use hardware clock 1
	int PI_HPWM_ILLEGAL = -100; // illegal, PWM in use for main clock
	int PI_BAD_DATABITS = -101; // serial data bits not 1-32
	int PI_BAD_STOPBITS = -102; // serial (half) stop bits not 2-8
	int PI_MSG_TOOBIG = -103; // socket/pipe message too big
	int PI_BAD_MALLOC_MODE = -104; // bad memory allocation mode
	int PI_TOO_MANY_SEGS = -105; // too many I2C transaction segments
	int PI_BAD_I2C_SEG = -106; // an I2C transaction segment failed
	int PI_BAD_SMBUS_CMD = -107; // SMBus command not supported by driver
	int PI_NOT_I2C_GPIO = -108; // no bit bang I2C in progress on GPIO
	int PI_BAD_I2C_WLEN = -109; // bad I2C write length
	int PI_BAD_I2C_RLEN = -110; // bad I2C read length
	int PI_BAD_I2C_CMD = -111; // bad I2C command
	int PI_BAD_I2C_BAUD = -112; // bad I2C baud rate, not 50-500k
	int PI_CHAIN_LOOP_CNT = -113; // bad chain loop count
	int PI_BAD_CHAIN_LOOP = -114; // empty chain loop
	int PI_CHAIN_COUNTER = -115; // too many chain counters
	int PI_BAD_CHAIN_CMD = -116; // bad chain command
	int PI_BAD_CHAIN_DELAY = -117; // bad chain delay micros
	int PI_CHAIN_NESTING = -118; // chain counters nested too deeply
	int PI_CHAIN_TOO_BIG = -119; // chain is too long
	int PI_DEPRECATED = -120; // deprecated function removed
	int PI_BAD_SER_INVERT = -121; // bit bang serial invert not 0 or 1
	int PI_BAD_EDGE = -122; // bad ISR edge value, not 0-2
	int PI_BAD_ISR_INIT = -123; // bad ISR initialisation
	int PI_BAD_FOREVER = -124; // loop forever must be last command
	int PI_BAD_FILTER = -125; // bad filter parameter
	int PI_BAD_PAD = -126; // bad pad number
	int PI_BAD_STRENGTH = -127; // bad pad drive strength
	int PI_FIL_OPEN_FAILED = -128; // file open failed
	int PI_BAD_FILE_MODE = -129; // bad file mode
	int PI_BAD_FILE_FLAG = -130; // bad file flag
	int PI_BAD_FILE_READ = -131; // bad file read
	int PI_BAD_FILE_WRITE = -132; // bad file write
	int PI_FILE_NOT_ROPEN = -133; // file not open for read
	int PI_FILE_NOT_WOPEN = -134; // file not open for write
	int PI_BAD_FILE_SEEK = -135; // bad file seek
	int PI_NO_FILE_MATCH = -136; // no files match pattern
	int PI_NO_FILE_ACCESS = -137; // no permission to access file
	int PI_FILE_IS_A_DIR = -138; // file is a directory
	int PI_BAD_SHELL_STATUS = -139;// bad shell return status
	int PI_BAD_SCRIPT_NAME = -140; // bad script name
	int PI_BAD_SPI_BAUD = -141; // bad SPI baud rate, not 50-500k
	int PI_NOT_SPI_GPIO = -142; // no bit bang SPI in progress on GPIO
	int PI_BAD_EVENT_ID = -143; // bad event id
}