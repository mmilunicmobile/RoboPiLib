package uk.pigpioj;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;

@SuppressWarnings("unused")
public class PigpioSocket implements PigpioInterface {
	static final Logger LOGGER = Logger.getLogger(PigpioSocket.class.getName());

	private static final int DEFAULT_TIMEOUT_MS = 30_000;
	private static final int NOTIFICATION_HANDLE_NOT_SET = -1;

	static final int DEFAULT_PORT = 8888;

	// Notification flags
	private static final int PI_NTFY_FLAGS_WDOG = 1 << 5;
	private static final int PI_NTFY_FLAGS_ALIVE = 1 << 6;
	private static final int PI_NTFY_FLAGS_EVENT = 1 << 7;

	// Commands
	/**
	 * GPIO mode set<br>
	 * Request: <code>gpio mode 0</code><br>
	 * Response: <code>- - 0 -</code>
	 */
	private static final int PI_CMD_MODES = 0;
	/**
	 * GPIO mode get<br>
	 * Request: <code>gpio 0 0 -</code><br>
	 * Response: <code>- - mode -</code>
	 */
	private static final int PI_CMD_MODEG = 1;
	/**
	 * GPIO set pull up/down<br>
	 * Request: <code>gpio pud 0 -</code><br>
	 * Response: <code>- - 0 -</code>
	 */
	private static final int PI_CMD_PUD = 2;
	/**
	 * GPIO read<br>
	 * Request: <code>gpio 0 0 =</code><br>
	 * Response: <code>- - level -</code>
	 */
	private static final int PI_CMD_READ = 3;
	/**
	 * GPIO write<br>
	 * Response: <code>gpio level 0 -</code><br>
	 * Response: <code>- - 0 -</code>
	 */
	private static final int PI_CMD_WRITE = 4;
	/**
	 * PWM set duty cycle<br>
	 * Request: <code>gpio dutycycle 0 -</code><br>
	 * Response: <code>- - 0 -</code>
	 */
	private static final int PI_CMD_PWM = 5;
	/**
	 * PWM set range<br>
	 * Request: <code>gpio range 0 -</code><br>
	 * Response: <code>- - 0 -</code>
	 */
	private static final int PI_CMD_PRS = 6;
	/**
	 * PWM set frequency<br>
	 * Request: <code>gpio frequency 0 -</code><br>
	 * Response: <code>- - 0 -</code>
	 */
	private static final int PI_CMD_PFS = 7;
	/**
	 * Servo set pulse width<br>
	 * Request: <code>gpio pulsewidth 0 -</code><br>
	 * Response: <code>- - 0 -</code>
	 */
	private static final int PI_CMD_SERVO = 8;
	/**
	 * Set GPIO watchdog<br>
	 * Request: <code>gpio timeout 0 -</code><br>
	 * Response: <code>- - 0 -</code>
	 */
	private static final int PI_CMD_WDOG = 9;
	/**
	 * Read GPIO bank 1, i.e. GPIOs 0-31<br>
	 * Request: <code>0 0 0 -</code><br>
	 * Response: <code>- - bits -</code>
	 */
	private static final int PI_CMD_BR1 = 10;
	/**
	 * Read GPIO bank 2, i.e. GPIOs 32-63<br>
	 * Request: <code>0 0 0 -</code><br>
	 * Response: <code>- - bits -</code>
	 */
	private static final int PI_CMD_BR2 = 11;
	private static final int PI_CMD_BC1 = 12; // bits 0 0 - (Clear GPIO bank 1)
	private static final int PI_CMD_BC2 = 13; // bits 0 0 - (Clear GPIO bank 2)
	private static final int PI_CMD_BS1 = 14; // bits 0 0 - (Set GPIO bank 1)
	private static final int PI_CMD_BS2 = 15; // bits 0 0 - (Set GPIO bank 2)
	private static final int PI_CMD_TICK = 16; // 0 0 0 - (?)
	private static final int PI_CMD_HWVER = 17; // 0 0 0 - (Get Pi Hardware Revision)
	private static final int PI_CMD_NO = 18; // 0 0 0 - (Notify Open)
	private static final int PI_CMD_NB = 19; // handle bits 0 - (Notify Begin)
	private static final int PI_CMD_NP = 20; // handle 0 0 - (Notify Pause)
	private static final int PI_CMD_NC = 21; // handle 0 0 - (Notify Close)
	private static final int PI_CMD_PRG = 22; // gpio 0 0 - (PWM get range)
	private static final int PI_CMD_PFG = 23; // gpio 0 0 - (PWM get frequency)
	private static final int PI_CMD_PRRG = 24; // gpio 0 0 - (PWM get real range)
	private static final int PI_CMD_HELP = 25; // N/A N/A N/A N/A
	private static final int PI_CMD_PIGPV = 26; // 0 0 0 - (Get pigpio version)
	private static final int PI_CMD_WVCLR = 27; // 0 0 0 -
	private static final int PI_CMD_WVAG = 28; // 0 0 12*X gpioPulse_t pulse[X]
	private static final int PI_CMD_WVAS = 29; // gpio baud 12+X uint32_t databits uint32_t stophalfbits uint32_t offset
												// uint8_t data[X]
	private static final int PI_CMD_WVGO = 30; // 0 0 0 -
	private static final int PI_CMD_WVGOR = 31; // 0 0 0 -
	private static final int PI_CMD_WVBSY = 32; // 0 0 0 -
	private static final int PI_CMD_WVHLT = 33; // 0 0 0 -
	private static final int PI_CMD_WVSM = 34; // subcmd 0 0 -
	private static final int PI_CMD_WVSP = 35; // subcmd 0 0 -
	private static final int PI_CMD_WVSC = 36; // subcmd 0 0 -
	private static final int PI_CMD_TRIG = 37; // gpio pulselen 4 uint32_t level
	private static final int PI_CMD_PROC = 38; // 0 0 X uint8_t text[X]
	private static final int PI_CMD_PROCD = 39; // script_id 0 0 -
	private static final int PI_CMD_PROCR = 40; // script_id 0 4*X uint32_t pars[X]
	private static final int PI_CMD_PROCS = 41; // script_id 0 0 -
	private static final int PI_CMD_SLRO = 42; // gpio baud 4 uint32_t databits
	private static final int PI_CMD_SLR = 43; // gpio count 0 -
	private static final int PI_CMD_SLRC = 44; // gpio 0 0 -
	/**
	 * Get script status<br>
	 * Request: <code>script_id 0 0 -</code><br>
	 * Response: <code>- - X+4 uint32_t status; uint8_t data[X]</code>
	 */
	private static final int PI_CMD_PROCP = 45; //
	private static final int PI_CMD_MICS = 46; // micros 0 0 -
	private static final int PI_CMD_MILS = 47; // millis 0 0 -
	private static final int PI_CMD_PARSE = 48; // N/A N/A N/A N/A
	private static final int PI_CMD_WVCRE = 49; // 0 0 0
	private static final int PI_CMD_WVDEL = 50; // wave_id 0 0
	private static final int PI_CMD_WVTX = 51; // wave_id 0 0
	private static final int PI_CMD_WVTXR = 52; // wave_id 0 0
	private static final int PI_CMD_WVNEW = 53; // 0 0 0 -
	// I2C Commands
	/**
	 * I2C open<br>
	 * Request: <code>bus device 4 uint32_t flags-</code><br>
	 * Response: <code>- - handle -</code>
	 */
	private static final int PI_CMD_I2CO = 54;
	/**
	 * I2C close<br>
	 * Request: <code>handle 0 0 -</code><br>
	 * Response: <code>- - 0 -</code>
	 */
	private static final int PI_CMD_I2CC = 55;
	/**
	 * I2C read device<br>
	 * Request: <code>handle count 0 -</code><br>
	 * Response: <code>- - X uint8_t data[X]</code>
	 */
	private static final int PI_CMD_I2CRD = 56;
	/**
	 * I2C write device<br>
	 * Request: <code>handle 0 X uint8_t data[X]</code><br>
	 * Response: <code>- - 0 -</code>
	 */
	private static final int PI_CMD_I2CWD = 57;
	/**
	 * I2C write quick<br>
	 * Request: <code>handle bit 0 -</code><br>
	 * Response: <code>- - 0 -</code>
	 */
	private static final int PI_CMD_I2CWQ = 58;
	/**
	 * I2C read byte<br>
	 * Request: <code>handle 0 0 -</code><br>
	 * Response: <code>- - byte value -</code>
	 */
	private static final int PI_CMD_I2CRS = 59;
	/**
	 * I2C write byte<br>
	 * Request: <code>handle byte 0 -</code><br>
	 * Response: <code>- - 0 -</code>
	 */
	private static final int PI_CMD_I2CWS = 60;
	/**
	 * I2C read byte data<br>
	 * Request: <code>handle register 0 -</code><br>
	 * Response: <code>- - byte value -</code>
	 */
	private static final int PI_CMD_I2CRB = 61;
	/**
	 * I2C write byte data<br>
	 * Request: <code>handle register 4 uint32_t byte</code><br>
	 * Response: <code>- - 0 -</code>
	 */
	private static final int PI_CMD_I2CWB = 62;
	/**
	 * I2C read word data<br>
	 * Request: <code>handle register 0 -</code><br>
	 * Response: <code>- - word value -</code>
	 */
	private static final int PI_CMD_I2CRW = 63;
	/**
	 * I2C write word data<br>
	 * Request: <code>?handle register 4 uint32_t word</code><br>
	 * Response: <code>- - 0 -</code>
	 */
	private static final int PI_CMD_I2CWW = 64;
	/**
	 * I2C read block data<br>
	 * Request: <code>handle register 0 -</code><br>
	 * Response: <code>- - X uint8_t data[X]</code>
	 */
	private static final int PI_CMD_I2CRK = 65;
	/**
	 * I2C write block data<br>
	 * Request: <code>handle register X uint8_t bvs[X]</code><br>
	 * Response: <code>- - 0 -</code>
	 */
	private static final int PI_CMD_I2CWK = 66;
	/**
	 * I2C read I2C block data<br>
	 * Response: <code>handle register 4 uint32_t num</code><br>
	 * Response: <code>- - X uint8_t data[X]</code>
	 */
	private static final int PI_CMD_I2CRI = 67;
	/**
	 * I2C write I2C block data<br>
	 * Request: <code>handle register X uint8_t bvs[X]</code><br>
	 * Response: <code>- - 0 -</code>
	 */
	private static final int PI_CMD_I2CWI = 68;
	/**
	 * I2C process call<br>
	 * Request: <code>handle register 4 uint32_t word</code><br>
	 * Response: <code>- - word value -</code>
	 */
	private static final int PI_CMD_I2CPC = 69;
	/**
	 * I2C block process call<br>
	 * Request: <code>handle register X uint8_t data[X]</code><br>
	 * Response: <code>- - X uint8_t data[X]</code>
	 */
	private static final int PI_CMD_I2CPK = 70;
	/**
	 * This function executes a sequence of I2C operations. The operations to be
	 * performed are specified by the contents of inBuf which contains the
	 * concatenated command codes and associated data.
	 *
	 * Request: <code>handle 0 X uint8_t data[X]</code><br>
	 * Response: ??
	 */
	private static final int PI_CMD_I2CZ = 92;

	// SPI Commands
	/**
	 * SPI open<br>
	 * Request: <code>channel baud 4 uint32_t flags</code><br>
	 * Response: <code>- - handle -</code>
	 */
	private static final int PI_CMD_SPIO = 71;
	/**
	 * SPI clode<br>
	 * Request: <code>handle 0 0 -</code><br>
	 * Response: <code>- - 0 -</code>
	 */
	private static final int PI_CMD_SPIC = 72;
	/**
	 * SPI read<br>
	 * Request: <code>handle count 0 -</code><br>
	 * Response: <code>- - X uint8_t data[X]</code>
	 */
	private static final int PI_CMD_SPIR = 73;
	/**
	 * SPI write<br>
	 * Request: <code>handle 0 X uint8_t data[X]</code><br>
	 * Response: <code>- - 0 -</code>
	 */
	private static final int PI_CMD_SPIW = 74;
	/**
	 * SPI Transfer<br>
	 * Request: <code>handle 0 X uint8_t data[X]</code><br>
	 * Response: <code>- - X uint8_t data[X]</code>
	 */
	private static final int PI_CMD_SPIX = 75;

	private static final int PI_CMD_SERO = 76; // baud flags X uint8_t device[X]
	private static final int PI_CMD_SERC = 77; // handle 0 0 -
	private static final int PI_CMD_SERRB = 78; // handle 0 0 -
	private static final int PI_CMD_SERWB = 79; // handle byte 0 -
	private static final int PI_CMD_SERR = 80; // handle count 0 -
	private static final int PI_CMD_SERW = 81; // handle 0 X uint8_t data[X]
	private static final int PI_CMD_SERDA = 82; // handle 0 0 -
	private static final int PI_CMD_GDC = 83; // gpio 0 0 -
	private static final int PI_CMD_GPW = 84; // gpio 0 0 -
	private static final int PI_CMD_HC = 85; // gpio frequency 0 -
	private static final int PI_CMD_HP = 86; // gpio frequency 4 uint32_t dutycycle
	private static final int PI_CMD_CF1 = 87; // arg1 arg2 X uint8_t argx[X]
	private static final int PI_CMD_CF2 = 88; // arg1 retMax X uint8_t argx[X]
	private static final int PI_CMD_BI2CC = 89; // sda 0 0 -
	private static final int PI_CMD_BI2CO = 90; // sda scl 4 uint32_t baud
	private static final int PI_CMD_BI2CZ = 91; // sda 0 X uint8_t data[X]
	private static final int PI_CMD_WVCHA = 93; // 0 0 X uint8_t data[X]
	private static final int PI_CMD_SLRI = 94; // gpio invert 0 -
	private static final int PI_CMD_CGI = 95; // 0 0 0 -
	private static final int PI_CMD_CSI = 96; // config 0 0 -
	private static final int PI_CMD_FG = 97; // gpio steady 0 - (Set glitch filter)
	private static final int PI_CMD_FN = 98; // gpio steady 4 uint32_t active (Set noise filter)
	private static final int PI_CMD_NOIB = 99; // 0 0 0 - (Notify Open In Band)
	private static final int PI_CMD_WVTXM = 100; // wave_id mode 0 -
	private static final int PI_CMD_WVTAT = 101; // - - 0 -
	private static final int PI_CMD_PADS = 102; // pad strength 0 -
	private static final int PI_CMD_PADG = 103; // pad 0 0 -
	private static final int PI_CMD_FO = 104; // mode 0 X uint8_t file[X]
	private static final int PI_CMD_FC = 105; // handle 0 0 - (File close)
	private static final int PI_CMD_FR = 106; // handle count 0 -
	private static final int PI_CMD_FW = 107; // handle 0 X uint8_t data[X]
	private static final int PI_CMD_FS = 108; // handle offset 4 uint32_t from
	private static final int PI_CMD_FL = 109; // count 0 X uint8_t pattern[X]
	private static final int PI_CMD_SHELL = 110; // len(name) 0 len(name)+1+len(string) uint8_t name[len(name)] uint8_t
													// null (0) uint8_t string[len(string)]
	private static final int PI_CMD_BSPIC = 111; // CS 0 0 -
	private static final int PI_CMD_BSPIO = 112; // CS 0 20 uint32_t MISO uint32_t MOSI uint32_t SCLK uint32_t baud
													// uint32_t spi_flags
	private static final int PI_CMD_BSPIX = 113; // CS 0 X uint8_t data[X]
	/**
	 * I2C/SPI as slave transfer<br>
	 * Request: <code>control 0 X uint8_t data[X]</code><br>
	 * Response: <code>- - X+4 uint32_t status; uint8_t data[X]</code>
	 */
	private static final int PI_CMD_BSCX = 114;
	private static final int PI_CMD_EVM = 115; // handle bits 0 - (Event Monitor)
	private static final int PI_CMD_EVT = 116; // event 0 0 - (Event Trigger)
	private static final int PI_CMD_PROCU = 117;
	private static final int PI_CMD_WVCAP = 118;

	/* bbI2CZip and i2cZip commands */
	private static final byte PI_I2C_END = 0;
	private static final byte PI_I2C_ESC = 1;
	private static final byte PI_I2C_START = 2;
	private static final byte PI_I2C_COMBINED_ON = 2;
	private static final byte PI_I2C_STOP = 3;
	private static final byte PI_I2C_COMBINED_OFF = 3;
	private static final byte PI_I2C_ADDR = 4;
	private static final byte PI_I2C_FLAGS = 5;
	private static final byte PI_I2C_READ = 6;
	private static final byte PI_I2C_WRITE = 7;

	private static final int PIGPIOJ_CMD_EXIT = -999;

	/*-
	 * pigpiod_if2
	 * Error Codes
	typedef enum {
	  pigif_bad_send = -2000,
	  pigif_bad_recv = -2001,
	  pigif_bad_getaddrinfo = -2002,
	  pigif_bad_connect = -2003,
	  pigif_bad_socket = -2004,
	  pigif_bad_noib = -2005,
	  pigif_duplicate_callback = -2006,
	  pigif_bad_malloc = -2007,
	  pigif_bad_callback = -2008,
	  pigif_notify_failed = -2009,
	  pigif_callback_not_found = -2010,
	  pigif_unconnected_pi = -2011,
	  pigif_too_many_pis = -2012,
	} pigifError_t;
	*/

	private final int timeoutMs;
	private final BlockingQueue<ResponseMessage> messageQueue;
	private final Map<Integer, PigpioCallback> callbacks;
	private final AtomicInteger notificationHandle = new AtomicInteger(NOTIFICATION_HANDLE_NOT_SET);
	private EventLoopGroup workerGroup;
	private Channel messageChannel;
	private Channel notificationChannel;
	private ChannelFuture lastWriteFuture;
	private int monitorMask;
	private int lastGpioLevelMask;

	public PigpioSocket() {
		this(DEFAULT_TIMEOUT_MS);
	}

	public PigpioSocket(int timeoutMs) {
		this.timeoutMs = timeoutMs;

		messageQueue = new LinkedBlockingQueue<>();
		callbacks = new HashMap<>();
	}

	public void connect(String host) {
		connect(host, DEFAULT_PORT);
	}

	public void connect(String host, int port) {
		workerGroup = new NioEventLoopGroup();

		try {
			final MessageEncoder me = new MessageEncoder();

			final ResponseHandler rh = new ResponseHandler(this::messageReceived);
			Bootstrap b1 = new Bootstrap();
			b1.group(workerGroup).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast(new ResponseDecoder(), me, rh);
				}
			}).option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);

			// Connect the main request-response message channel
			messageChannel = b1.connect(host, port).sync().channel();

			final NotificationHandler nh = new NotificationHandler(this::notificationReceived);
			Bootstrap b2 = new Bootstrap();
			b2.group(workerGroup).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast(new NotificationDecoder(), me, rh, nh);
				}
			}).option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);

			// Connect the async notification channel
			notificationChannel = b2.connect(host, port).sync().channel();

			// Enable notification messages in pigpiod
			notificationChannel.writeAndFlush(new Message(PI_CMD_NOIB, 0, 0));

			LOGGER.fine("Connected to " + host + " using port " + port);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error connecting to " + host + ":" + port, e);
			try {
				workerGroup.shutdownGracefully().sync();
			} catch (Exception e1) {
				// Ignore
			}
			throw new RuntimeException("Error connecting to pigpiod at " + host + ":" + port, e);
		}
	}

	@Override
	public void close() {
		if (messageChannel == null || !messageChannel.isOpen()) {
			return;
		}

		// Send a poison "quit" message to the message queue to safely wake it up
		messageQueue.offer(new ResponseMessage(PIGPIOJ_CMD_EXIT, 0, 0, 0));

		messageChannel.close();
		notificationChannel.close();

		try {
			messageChannel.closeFuture().sync();
			notificationChannel.closeFuture().sync();

			// Wait until all messages are flushed before closing the channel.
			if (lastWriteFuture != null) {
				lastWriteFuture.sync();
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error: " + e, e);
		} finally {
			try {
				workerGroup.shutdownGracefully().sync();
			} catch (Exception e) {
				// Ignore
			}
		}
	}

	void messageReceived(ResponseMessage msg) {
		LOGGER.finer("messageReceived(" + msg + ")");

		// A hack as notifications are sent via the notification channel which has a
		// different message structure
		if (msg.cmd == PI_CMD_NOIB) {
			notificationHandle.set((int) msg.res);
			return;
		}

		messageQueue.offer(msg);
	}

	void notificationReceived(NotificationMessage msg) {
		if (msg.flags == 0) {
			int changed_level_mask = lastGpioLevelMask ^ msg.level;
			LOGGER.fine("changed_level_mask: " + changed_level_mask);
			LOGGER.finer(() -> "changed_level_mask: \n" //
					+ decodeMask(changed_level_mask) + "\n"
					+ "...|....;....|....;....|....;....|....;....|....;....|....;....|");

			lastGpioLevelMask = msg.level;
			callbacks.entrySet().stream().filter(entry -> (1 << entry.getKey().intValue() & changed_level_mask) != 0)
					.forEach(entry -> entry.getValue().callback(entry.getKey().intValue(),
							(1 << entry.getKey().intValue() & msg.level) != 0, msg.epochTime, msg.nanoTime));
		} else {
			if ((msg.flags & PI_NTFY_FLAGS_WDOG) != 0) {
				LOGGER.finer("WDOG notification message: " + msg);
			}
			if ((msg.flags & PI_NTFY_FLAGS_ALIVE) != 0) {
				LOGGER.finer("ALIVE notification message: " + msg);
			}
			if ((msg.flags & PI_NTFY_FLAGS_EVENT) != 0) {
				LOGGER.finer("EVENT notification message: " + msg);
			}
		}
	}

	private synchronized ResponseMessage sendMessage(Message message) {
		ResponseMessage rm = null;

		try {
			lastWriteFuture = messageChannel.writeAndFlush(message);

			// Loop until we get the expected response message
			while (!Thread.interrupted()) {
				rm = messageQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);

				if (rm != null) {
					if (rm.cmd == PIGPIOJ_CMD_EXIT) {
						rm = null;
						break;
					}
					if (rm.cmd == message.cmd) {
						break;
					}

					// Shouldn't happen
					LOGGER.warning("Unexpected response: " + rm + ". Was expecting " + message.cmd);
				} else {
					String msg = "Timeout waiting for response to command " + message.cmd;
					LOGGER.severe(msg);
					throw new TimeoutException(msg);
				}
			}
		} catch (InterruptedException e) {
			// Ignore or rethrow??
			// Thread.currentThread().interrupt();
			LOGGER.log(Level.WARNING, "Interrupted waiting on message queue...: " + e, e);
		}

		return rm;
	}

	@Override
	public int enableListener(int gpio, int edge, PigpioCallback callback) {
		if (notificationHandle.get() == NOTIFICATION_HANDLE_NOT_SET) {
			LOGGER.warning("Error, notification handle not set");
		}

		int monitor_bit = 1 << gpio;
		if ((monitorMask & monitor_bit) != 0) {
			LOGGER.warning("GPIO " + gpio + " is already being monitored");
			return PigpioConstants.SUCCESS;
		}
		if (sendMessage(new Message(PI_CMD_NB, notificationHandle.get(), monitorMask | monitor_bit)) == null) {
			return PigpioConstants.ERROR;
		}
		monitorMask |= monitor_bit;
		callbacks.put(Integer.valueOf(gpio), callback);

		return PigpioConstants.SUCCESS;
	}

	@Override
	public int disableListener(int gpio) {
		if (notificationHandle.get() == NOTIFICATION_HANDLE_NOT_SET) {
			LOGGER.warning("Error, notification handle not set");
		}

		int monitor_bit = 1 << gpio;
		if ((monitorMask & monitor_bit) == 0) {
			LOGGER.warning("GPIO " + gpio + " isn't being monitored");
			return PigpioConstants.SUCCESS;
		}
		if (sendMessage(new Message(PI_CMD_NB, notificationHandle.get(), monitorMask & ~monitor_bit)) == null) {
			return PigpioConstants.ERROR;
		}
		monitorMask &= ~monitor_bit;
		callbacks.remove(Integer.valueOf(gpio));

		return PigpioConstants.SUCCESS;
	}

	@Override
	public int getVersion() {
		ResponseMessage message = sendMessage(new Message(PI_CMD_PIGPV, 0, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int getHardwareRevision() {
		ResponseMessage message = sendMessage(new Message(PI_CMD_HWVER, 0, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int getMode(int gpio) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_MODEG, gpio, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int setMode(int gpio, int mode) {
		if (sendMessage(new Message(PI_CMD_MODES, gpio, mode)) == null) {
			return PigpioConstants.ERROR;
		}

		return PigpioConstants.SUCCESS;
	}

	@Override
	public int read(int gpio) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_READ, gpio, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int write(int gpio, boolean level) {
		if (sendMessage(
				new Message(PI_CMD_WRITE, gpio, level ? PigpioConstants.PI_ON : PigpioConstants.PI_OFF)) == null) {
			return PigpioConstants.ERROR;
		}

		return PigpioConstants.SUCCESS;
	}

	@Override
	public int getPWMDutyCycle(int gpio) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_GDC, gpio, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int setPWMDutyCycle(int gpio, int dutyCycle) {
		if (sendMessage(new Message(PI_CMD_PWM, gpio, dutyCycle)) == null) {
			return PigpioConstants.ERROR;
		}

		return PigpioConstants.SUCCESS;
	}

	@Override
	public int getPWMRange(int gpio) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_PRG, gpio, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int setPWMRange(int gpio, int range) {
		if (sendMessage(new Message(PI_CMD_PRS, gpio, range)) == null) {
			return PigpioConstants.ERROR;
		}

		return PigpioConstants.SUCCESS;
	}

	@Override
	public int getPWMRealRange(int gpio) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_PRRG, gpio, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int getPWMFrequency(int gpio) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_PFG, gpio, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int setPWMFrequency(int gpio, int frequency) {
		if (sendMessage(new Message(PI_CMD_PFS, gpio, frequency)) == null) {
			return PigpioConstants.ERROR;
		}

		return PigpioConstants.SUCCESS;
	}

	@Override
	public int setPullUpDown(int gpio, int pud) {
		if (sendMessage(new Message(PI_CMD_PUD, gpio, pud)) == null) {
			return PigpioConstants.ERROR;
		}

		return PigpioConstants.SUCCESS;
	}

	@Override
	public int getServoPulseWidth(int gpio) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_GPW, gpio, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int setServoPulseWidth(int gpio, int pulseWidth) {
		if (sendMessage(new Message(PI_CMD_SERVO, gpio, pulseWidth)) == null) {
			return PigpioConstants.ERROR;
		}

		return PigpioConstants.SUCCESS;
	}

	@Override
	public int noiseFilter(int gpio, int steadyMs, int activeMs) {
		if (sendMessage(new Message(PI_CMD_FN, gpio, steadyMs, new UIntMessageExtension(activeMs))) == null) {
			return PigpioConstants.ERROR;
		}

		return PigpioConstants.SUCCESS;
	}

	@Override
	public int glitchFilter(int gpio, int steadyMs) {
		if (sendMessage(new Message(PI_CMD_FG, gpio, steadyMs)) == null) {
			return PigpioConstants.ERROR;
		}

		return PigpioConstants.SUCCESS;
	}

	@Override
	public int hardwareClock(int gpio, int clockFreq) {
		if (sendMessage(new Message(PI_CMD_HC, gpio, clockFreq)) == null) {
			return PigpioConstants.ERROR;
		}

		return PigpioConstants.SUCCESS;
	}

	@Override
	public int hardwarePwm(int gpio, int pwmFreq, int pwmDuty) {
		if (sendMessage(new Message(PI_CMD_HP, gpio, pwmFreq, new UIntMessageExtension(pwmDuty))) == null) {
			return PigpioConstants.ERROR;
		}

		return PigpioConstants.SUCCESS;
	}

	// I2C

	@Override
	public int i2cOpen(int i2cBus, int i2cAddr, int i2cFlags) {
		ResponseMessage message = sendMessage(
				new Message(PI_CMD_I2CO, i2cBus, i2cAddr, new UIntMessageExtension(i2cFlags)));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int i2cClose(int handle) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_I2CC, handle));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int i2cWriteQuick(int handle, int bit) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_I2CWQ, handle, bit));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int i2cReadByte(int handle) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_I2CRS, handle, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int i2cWriteByte(int handle, int bVal) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_I2CWS, handle, bVal));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int i2cReadByteData(int handle, int i2cReg) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_I2CRB, handle, i2cReg));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int i2cWriteByteData(int handle, int i2cReg, int bVal) {
		ResponseMessage message = sendMessage(
				new Message(PI_CMD_I2CWB, handle, i2cReg, new UIntMessageExtension(bVal)));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int i2cReadWordData(int handle, int i2cReg) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_I2CRW, handle, i2cReg));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int i2cWriteWordData(int handle, int i2cReg, int wVal) {
		ResponseMessage message = sendMessage(
				new Message(PI_CMD_I2CWW, handle, i2cReg, new UIntMessageExtension(wVal)));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int i2cProcessCall(int handle, int i2cReg, int wVal) {
		ResponseMessage message = sendMessage(
				new Message(PI_CMD_I2CPC, handle, i2cReg, new UIntMessageExtension(wVal)));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int i2cReadBlockData(int handle, int i2cReg, byte[] buf) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_I2CRK, handle, i2cReg));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		ByteArrayResponseMessage bam = (ByteArrayResponseMessage) message;
		System.arraycopy(bam.data, 0, buf, 0, bam.data.length);

		return (int) message.res;
	}

	@Override
	public int i2cWriteBlockData(int handle, int i2cReg, byte[] buf, int count) {
		ResponseMessage message = sendMessage(
				new Message(PI_CMD_I2CWK, handle, i2cReg, new ByteArrayMessageExtension(count, buf)));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int i2cBlockProcessCall(int handle, int i2cReg, byte[] buf, int count) {
		ResponseMessage message = sendMessage(
				new Message(PI_CMD_I2CPK, handle, i2cReg, new ByteArrayMessageExtension(count, buf)));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		ByteArrayResponseMessage bam = (ByteArrayResponseMessage) message;
		System.arraycopy(bam.data, 0, buf, 0, bam.data.length);

		return (int) message.res;
	}

	@Override
	public int i2cReadI2CBlockData(int handle, int i2cReg, byte[] buf, int count) {
		ResponseMessage message = sendMessage(
				new Message(PI_CMD_I2CRI, handle, i2cReg, new UIntMessageExtension(count)));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		if (!(message instanceof ByteArrayResponseMessage)) {
			LOGGER.severe("Expected ByteArrayResponseMessage, got " + message.getClass().getName() + ": " + message);
			throw new RuntimeException(
					"Expected ByteArrayResponseMessage, got " + message.getClass().getName() + ": " + message);
		}

		ByteArrayResponseMessage bam = (ByteArrayResponseMessage) message;
		System.arraycopy(bam.data, 0, buf, 0, bam.data.length);

		return (int) message.res;
	}

	@Override
	public int i2cWriteI2CBlockData(int handle, int i2cReg, byte[] buf, int count) {
		ResponseMessage message = sendMessage(
				new Message(PI_CMD_I2CWI, handle, i2cReg, new ByteArrayMessageExtension(count, buf)));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int i2cReadDevice(int handle, byte[] buffer, int count) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_I2CRD, handle, count));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		ByteArrayResponseMessage bam = (ByteArrayResponseMessage) message;
		System.arraycopy(bam.data, 0, buffer, 0, bam.data.length);

		return (int) message.res;
	}

	@Override
	public int i2cWriteDevice(int handle, byte[] buffer, int count) {
		ResponseMessage message = sendMessage(
				new Message(PI_CMD_I2CWD, handle, 0, new ByteArrayMessageExtension(count, buffer)));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int i2cSegments(int handle, PiI2CMessage[] segs, byte[] data) {
		/*-
		 * int i2c_zip(int pi, unsigned handle, char *inBuf, unsigned inLen, char *outBuf, unsigned outLen)
		 *
		 *     pi: >=0 (as returned by pigpio_start).
		 * handle: >=0, as returned by a call to i2cOpen
		 *  inBuf: pointer to the concatenated I2C commands, see below
		 *  inLen: size of command buffer
		 * outBuf: pointer to buffer to hold returned data
		 * outLen: size of output buffer
		 *
		 * res = i2cZip(p[1], buf, p[3], buf+(bufSize/2), bufSize/2);
		 * int i2cZip(unsigned handle, char *inBuf, unsigned inLen, char *outBuf, unsigned outLen)
		 *
		 * Example:
		 * Set address 0x53,  write 0x32,          read 6 bytes
		 * Set address 0x1E,  write 0x03,          read 7 bytes
		 * Set address 0x68,  Flags NO_START (0x4000),  write 0x1B 0x1A,     read 8 bytes
		 * End
		 * 0x04 0x53          0x07 0x01 0x32       0x06 0x06
		 * 0x04 0x1E          0x07 0x01 0x03       0x06 0x07
		 * 0x04 0x68          0x05 0x00 0x40            0x07 0x02 0x1B 0x1A  0x06 0x08
		 * 0x00
		 */

		byte[] buffer;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			int data_offset = 0;
			for (PiI2CMessage seg : segs) {
				// Note pigpio allows addr to be skipped if unchanged...
				baos.write(PI_I2C_ADDR);
				baos.write(seg.getAddr());

				// Handle 16-bit I2C flags that aren't just read (1) / write (0)
				if (seg.getFlags() > 1) {
					baos.write(PI_I2C_FLAGS);
					// LSB
					baos.write(seg.getFlags() & 0xff);
					// MSB
					baos.write((seg.getFlags() >> 8) & 0xff);
				}

				if ((seg.getFlags() & 0x01) == 0) {
					// Write
					baos.write(PI_I2C_WRITE);
					baos.write(seg.getLen());
					baos.write(data, data_offset, seg.getLen());
				} else {
					// Read
					baos.write(PI_I2C_READ);
					baos.write((byte) seg.getLen());
				}

				data_offset += seg.getLen();
			}

			baos.write(PI_I2C_END);
			baos.flush();

			buffer = baos.toByteArray();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Error writing to ByteArrayOutputStream: " + e, e);
			return -1;
		}

		/*-
		 * Doesn't remove read bytes from the capacity or handle flags
		 *
		// Assume that addresses are always sent even if the same
		int capacity = segs.length * 2; // ADDR command + addr
		// Every message is either read or write
		capacity += segs.length * 2; // READ/WRITE byte command + len
		// Handle capacity requirement for read / write data
		capacity += data.length;
		// Finally 1 byte for the END command
		capacity += 1;
		
		ByteBuffer bb = ByteBuffer.allocate(capacity);
		bb.order(ByteOrder.LITTLE_ENDIAN);

		int data_offset = 0;
		for (PiI2CMessage seg : segs) {
			bb.put(PI_I2C_ADDR);
			bb.put((byte) seg.getAddr());
			// TODO Handle I2C flags
			// bb.put(PI_I2C_FLAGS);
			// bb.putShort((short) seg.getFlags());
			if ((seg.getFlags() & 0x01) == 0) {
				// Write
				bb.put(PI_I2C_WRITE);
				bb.put((byte) seg.getLen());
				bb.put(data, data_offset, seg.getLen());
				data_offset += seg.getLen();
			} else {
				// Read
				bb.put(PI_I2C_READ);
				bb.put((byte) seg.getLen());
				data_offset += seg.getLen();
			}
		}
		bb.put(PI_I2C_END);

		bb.flip();

		byte[] buffer = new byte[bb.limit()];
		bb.get(buffer);
		*/

		ResponseMessage message = sendMessage(
				new Message(PI_CMD_I2CZ, handle, 0, new ByteArrayMessageExtension(buffer)));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		ByteArrayResponseMessage bam = (ByteArrayResponseMessage) message;
		byte[] resp_data = bam.data;

		int data_offset = 0;
		int resp_data_pos = 0;
		for (PiI2CMessage seg : segs) {
			if ((seg.getFlags() & 0x01) == 1) {
				// Read
				System.arraycopy(resp_data, resp_data_pos, data, data_offset, seg.getLen());
				resp_data_pos += seg.getLen();
			}
			data_offset += seg.getLen();
		}

		return (int) message.res;
	}

	// SPI

	@Override
	public int spiOpen(int spiChan, int baud, int spiFlags) {
		ResponseMessage message = sendMessage(
				new Message(PI_CMD_SPIO, spiChan, baud, new UIntMessageExtension(spiFlags)));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int spiClose(int handle) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_SPIC, handle));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int spiRead(int handle, byte[] buf, int count) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_SPIR, handle, count));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		ByteArrayResponseMessage bam = (ByteArrayResponseMessage) message;
		System.arraycopy(bam.data, 0, buf, 0, bam.data.length);

		return (int) message.res;
	}

	@Override
	public int spiWrite(int handle, byte[] buffer, int offset, int length) {
		ResponseMessage message = sendMessage(
				new Message(PI_CMD_SPIW, handle, 0, new ByteArrayMessageExtension(offset, length, buffer)));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int spiXfer(int handle, byte[] txBuf, byte[] rxBuf, int count) {
		ResponseMessage message = sendMessage(
				new Message(PI_CMD_SPIX, handle, 0, new ByteArrayMessageExtension(count, txBuf)));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		ByteArrayResponseMessage bam = (ByteArrayResponseMessage) message;
		System.arraycopy(bam.data, 0, rxBuf, 0, bam.data.length);

		return (int) message.res;
	}

	@Override
	public int gpioWaveClear() {
		ResponseMessage message = sendMessage(new Message(PI_CMD_WVCLR, 0, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int gpioWaveAddNew() {
		ResponseMessage message = sendMessage(new Message(PI_CMD_WVNEW, 0, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int gpioWaveAddGeneric(GpioPulse[] pulses) {
		// TODO Warning untested!
		ResponseMessage message = sendMessage(
				new Message(PI_CMD_WVAG, 0, pulses.length, new GpioPulseArrayMessageExtension(pulses)));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int gpioWaveAddSerial(int userGpio, int baud, int dataBits, int stopBits, int offset, byte[] str) {
		/*
		 * // TODO Create a message extension to carry dataBits, stopBits, offset, str
		 * length and str ResponseMessage message = sendMessage(new Message(PI_CMD_WVAG,
		 * userGpio, baud, new ByteArrayMessageExtension())); if (message == null) {
		 * return PigpioConstants.ERROR; }
		 *
		 * return (int) message.res;
		 */
		throw new UnsupportedOperationException();
	}

	@Override
	public int gpioWaveCreate() {
		ResponseMessage message = sendMessage(new Message(PI_CMD_WVCRE, 0, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int gpioWaveCreatePad(int pctCB, int pctBOOL, int pctTOOL) {
		ResponseMessage message = sendMessage(
				new Message(PI_CMD_WVCAP, pctCB, pctBOOL, new UIntMessageExtension(pctTOOL)));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int gpioWaveDelete(int waveId) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_WVDEL, waveId, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int gpioWaveTxSend(int waveId, int waveMode) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_WVTXM, waveId, waveMode));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int gpioWaveChain(byte[] buf) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_WVCHA, 0, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int gpioWaveTxAt() {
		ResponseMessage message = sendMessage(new Message(PI_CMD_WVTAT, 0, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int gpioWaveTxBusy() {
		ResponseMessage message = sendMessage(new Message(PI_CMD_WVBSY, 0, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int gpioWaveTxStop() {
		ResponseMessage message = sendMessage(new Message(PI_CMD_WVHLT, 0, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int gpioWaveGetMicros() {
		ResponseMessage message = sendMessage(new Message(PI_CMD_WVSM, 0, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int gpioWaveGetHighMicros() {
		ResponseMessage message = sendMessage(new Message(PI_CMD_WVSM, 1, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int gpioWaveGetMaxMicros() {
		ResponseMessage message = sendMessage(new Message(PI_CMD_WVSM, 2, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int gpioWaveGetPulses() {
		ResponseMessage message = sendMessage(new Message(PI_CMD_WVSP, 0, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int gpioWaveGetHighPulses() {
		ResponseMessage message = sendMessage(new Message(PI_CMD_WVSP, 1, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int gpioWaveGetMaxPulses() {
		ResponseMessage message = sendMessage(new Message(PI_CMD_WVSP, 2, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int gpioWaveGetCbs() {
		ResponseMessage message = sendMessage(new Message(PI_CMD_WVSC, 0, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int gpioWaveGetHighCbs() {
		ResponseMessage message = sendMessage(new Message(PI_CMD_WVSC, 1, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int gpioWaveGetMaxCbs() {
		ResponseMessage message = sendMessage(new Message(PI_CMD_WVSC, 2, 0));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int serOpen(String sertty, int baud, int serFlags) {
		ResponseMessage message = sendMessage(
				new Message(PI_CMD_SERO, baud, serFlags, new ByteArrayMessageExtension(sertty.getBytes())));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int serClose(int handle) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_SERC, handle));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int serWriteByte(int handle, int bVal) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_SERWB, handle, bVal));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int serReadByte(int handle) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_SERRB, handle));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int serWrite(int handle, byte[] buf, int count) {
		ResponseMessage message = sendMessage(
				new Message(PI_CMD_SERW, handle, 0, new ByteArrayMessageExtension(count, buf)));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	@Override
	public int serRead(int handle, byte[] buf, int count) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_SERR, handle, count));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		ByteArrayResponseMessage bam = (ByteArrayResponseMessage) message;
		System.arraycopy(bam.data, 0, buf, 0, bam.data.length);

		return (int) message.res;
	}

	@Override
	public int serDataAvailable(int handle) {
		ResponseMessage message = sendMessage(new Message(PI_CMD_SERDA, handle));
		if (message == null) {
			return PigpioConstants.ERROR;
		}

		return (int) message.res;
	}

	public static String decodeMask(int mask) {
		StringBuilder b = new StringBuilder(64);

		for (int i = 63; i >= 0; --i) {
			b.append(((1 << i) & mask) != 0 ? "1" : "0");
		}
		return b.toString();
	}

	/*
	 * typedef struct { uint32_t cmd; uint32_t p1; uint32_t p2; union { uint32_t p3;
	 * uint32_t ext_len; uint32_t res; }; } cmdCmd_t;
	 */
	static class Message {
		int cmd;
		long p1;
		long p2;
		long p3;
		MessageExtension extension;

		Message(int cmd, long p1) {
			this.cmd = cmd;
			this.p1 = p1;
		}

		Message(int cmd, long p1, long p2) {
			this.cmd = cmd;
			this.p1 = p1;
			this.p2 = p2;
		}

		Message(int cmd, long p1, long p2, MessageExtension extension) {
			this.cmd = cmd;
			this.p1 = p1;
			this.p2 = p2;
			this.p3 = extension.numBytes;
			this.extension = extension;
		}

		@Override
		public String toString() {
			return "Message [cmd=" + cmd + ", p1=" + p1 + ", p2=" + p2 + ", p3=" + p3 + ", extension=" + extension
					+ "]";
		}
	}

	static class ResponseMessage {
		int cmd;
		long p1;
		long p2;
		long res;

		ResponseMessage(int cmd, long p1, long p2, long res) {
			this.cmd = cmd;
			this.p1 = p1;
			this.p2 = p2;
			this.res = res;
		}

		@Override
		public String toString() {
			return "ResponseMessage [cmd=" + cmd + ", p1=" + p1 + ", p2=" + p2 + ", res=" + res + "]";
		}
	}

	static class ByteArrayResponseMessage extends ResponseMessage {
		byte[] data;

		public ByteArrayResponseMessage(int cmd, long p1, long p2, long res, byte[] data) {
			super(cmd, p1, p2, res);

			this.data = data;
		}
	}

	static class ScriptStatusResponseMessage extends ResponseMessage {
		long status;
		long[] pars;

		public ScriptStatusResponseMessage(int cmd, long p1, long p2, long res, long status, long[] pars) {
			super(cmd, p1, p2, res);

			this.status = status;
			this.pars = pars;
		}
	}

	static class BscXferResponseMessage extends ResponseMessage {
		long status;
		byte[] data;

		public BscXferResponseMessage(int cmd, long p1, long p2, long res, long status, byte[] data) {
			super(cmd, p1, p2, res);

			this.status = status;
			this.data = data;
		}
	}

	static abstract class MessageExtension {
		int numBytes;

		MessageExtension(int numBytes) {
			this.numBytes = numBytes;
		}

		abstract void encode(ByteBuf out);
	}

	static class UByteMessageExtension extends MessageExtension {
		short val;

		public UByteMessageExtension(short val) {
			super(1);

			this.val = val;
		}

		@Override
		public void encode(ByteBuf out) {
			out.writeByte(val);
		}

		@Override
		public String toString() {
			return "UByteMessageExtension [numBytes=" + numBytes + ", val=" + val + "]";
		}
	}

	static class UIntMessageExtension extends MessageExtension {
		long val;

		public UIntMessageExtension(long val) {
			super(4);

			this.val = val;
		}

		@Override
		public void encode(ByteBuf out) {
			out.writeIntLE((int) val);
		}

		@Override
		public String toString() {
			return "UIntMessageExtension [numBytes=" + numBytes + ", val=" + val + "]";
		}
	}

	static class ByteArrayMessageExtension extends MessageExtension {
		private int offset;
		byte[] data;

		public ByteArrayMessageExtension(byte[] data) {
			this(0, data.length, data);
		}

		public ByteArrayMessageExtension(int length, byte[] data) {
			this(0, length, data);
		}

		public ByteArrayMessageExtension(int offset, int length, byte[] data) {
			super(length);

			this.offset = offset;
			this.data = data;
		}

		@Override
		public void encode(ByteBuf out) {
			out.writeBytes(data, offset, numBytes);
		}

		@Override
		public String toString() {
			return "ByteArrayMessageExtension [numBytes=" + numBytes + ", data.length=" + data.length + "]";
		}
	}

	static class GpioPulseArrayMessageExtension extends MessageExtension {
		private GpioPulse[] pulses;

		public GpioPulseArrayMessageExtension(GpioPulse[] pulses) {
			// A GPIO pulse object has 3 4-byte integers
			super(pulses.length * 3 * 4);

			this.pulses = pulses;
		}

		@Override
		void encode(ByteBuf out) {
			for (GpioPulse pulse : pulses) {
				out.writeInt(pulse.getGpioOn() & 0xffffffff);
				out.writeInt(pulse.getGpioOff() & 0xffffffff);
				out.writeInt(pulse.getUsDelay() & 0xffffffff);
			}
		}
	}

	static class NotificationMessage {
		int seq; // unsigned short
		short flags; // unsigned short (bit mask)
		long tick; // Number of microseconds since system boot (unsigned int)
		int level; // Bit mask indicating the level of all GPIOs (unsigned int)
		long epochTime;
		long nanoTime;

		public NotificationMessage(int seq, short flags, long tick, int level, long epochTime, long nanoTime) {
			this.seq = seq;
			this.flags = flags;
			this.tick = tick;
			this.level = level;
			this.epochTime = epochTime;
			this.nanoTime = nanoTime;
		}

		@Override
		public String toString() {
			return "NotificationMessage [seq=" + seq + ", flags=0x" + Integer.toHexString(flags) + ", tick=" + tick
					+ ", level=0x" + Integer.toHexString(level) + ", epochTime=" + epochTime + ", nanoTime=" + nanoTime
					+ "]";
		}
	}

	@FunctionalInterface
	interface MessageListener<T> {
		void messageReceived(T message);
	}

	@Sharable
	static class MessageEncoder extends MessageToByteEncoder<Message> {
		@Override
		protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
			out.writeIntLE(msg.cmd);
			out.writeIntLE((int) msg.p1);
			out.writeIntLE((int) msg.p2);
			out.writeIntLE((int) msg.p3);
			if (msg.extension != null) {
				msg.extension.encode(out);
			}
		}
	}

	static class ResponseDecoder extends ByteToMessageDecoder {
		@Override
		protected void decode(ChannelHandlerContext context, ByteBuf buf, List<Object> out) {
			// Must be a minimum of 4 32-bit integers available to read
			if (buf.readableBytes() < 4 * 4) {
				return;
			}

			buf.markReaderIndex();

			int cmd = buf.readIntLE();
			long p1 = buf.readUnsignedIntLE();
			long p2 = buf.readUnsignedIntLE();
			long res = buf.readUnsignedIntLE();

			ResponseMessage message = null;
			switch (cmd) {
			case PI_CMD_SLR:
			case PI_CMD_I2CRD:
			case PI_CMD_I2CRK:
			case PI_CMD_I2CRI:
			case PI_CMD_I2CPK:
			case PI_CMD_SPIR:
			case PI_CMD_SPIX:
			case PI_CMD_SERR:
			case PI_CMD_CF2:
			case PI_CMD_BI2CZ:
			case PI_CMD_I2CZ:
			case PI_CMD_FR:
			case PI_CMD_FL:
			case PI_CMD_BSPIX:
				if (buf.readableBytes() < res) {
					buf.resetReaderIndex();
				} else {
					byte[] data = new byte[(int) res];
					buf.readBytes(data);
					message = new ByteArrayResponseMessage(cmd, p1, p2, res, data);
				}
				break;
			case PI_CMD_PROCP:
				if (buf.readableBytes() < res) {
					buf.resetReaderIndex();
				} else {
					long status = buf.readUnsignedIntLE();
					long[] pars = new long[(int) ((res - 4) / 4)];
					for (int i = 0; i < pars.length; i++) {
						pars[i] = buf.readUnsignedIntLE();
					}
					message = new ScriptStatusResponseMessage(cmd, p1, p2, res, status, pars);
				}
				break;
			case PI_CMD_BSCX:
				if (buf.readableBytes() < res) {
					buf.resetReaderIndex();
				} else {
					long status = buf.readUnsignedIntLE();
					byte[] data = new byte[(int) (res - 4)];
					buf.readBytes(data);
					message = new BscXferResponseMessage(cmd, p1, p2, res, status, data);
				}
				break;
			default:
				message = new ResponseMessage(cmd, p1, p2, res);
			}

			if (message != null) {
				out.add(message);
			}
		}
	}

	static class IntegerHeaderFrameDecoder extends ReplayingDecoder<ResponseMessage> {

		private boolean readLength;
		private int length;

		@Override
		protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
			if (!readLength) {
				length = buf.readInt();
				readLength = true;
				checkpoint();
			}

			if (readLength) {
				ByteBuf frame = buf.readBytes(length);
				readLength = false;
				checkpoint();
				out.add(frame);
			}
		}
	}

	static class NotificationDecoder extends ByteToMessageDecoder {
		private boolean notificationHandleSet;

		@Override
		protected void decode(ChannelHandlerContext context, ByteBuf in, List<Object> out) {
			long nano_time = System.nanoTime();
			long epoch_time = System.currentTimeMillis();

			if (!notificationHandleSet) {
				if (in.readableBytes() < 4 * 4) {
					return;
				}
				out.add(new ResponseMessage((int) in.readUnsignedIntLE(), in.readUnsignedIntLE(),
						in.readUnsignedIntLE(), in.readUnsignedIntLE()));
				notificationHandleSet = true;
				return;
			}

			// 2 shorts and 2 ints
			if (in.readableBytes() < (2 * 2 + 2 * 4)) {
				return;
			}

			out.add(new NotificationMessage(in.readUnsignedShortLE(), in.readShortLE(), in.readUnsignedIntLE(),
					in.readIntLE(), epoch_time, nano_time));
		}
	}

	@Sharable
	static class ResponseHandler extends SimpleChannelInboundHandler<ResponseMessage> {
		private MessageListener<ResponseMessage> listener;

		ResponseHandler(MessageListener<ResponseMessage> listener) {
			this.listener = listener;
		}

		@Override
		protected void channelRead0(ChannelHandlerContext context, ResponseMessage msg) {
			listener.messageReceived(msg);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
			LOGGER.log(Level.SEVERE, "exceptionCaught: " + cause, cause);
			context.close();
		}
	}

	static class NotificationHandler extends SimpleChannelInboundHandler<NotificationMessage> {
		private MessageListener<NotificationMessage> listener;

		NotificationHandler(MessageListener<NotificationMessage> listener) {
			this.listener = listener;
		}

		@Override
		protected void channelRead0(ChannelHandlerContext context, NotificationMessage msg) {
			listener.messageReceived(msg);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
			LOGGER.log(Level.SEVERE, "exceptionCaught: " + cause, cause);
			context.close();
		}
	}

	static class TimeoutException extends RuntimeException {
		private static final long serialVersionUID = 5767582299816127993L;

		public TimeoutException(String message) {
			super(message);
		}
	}
}
