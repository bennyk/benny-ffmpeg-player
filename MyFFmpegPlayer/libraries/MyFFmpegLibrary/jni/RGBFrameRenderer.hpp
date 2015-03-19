#ifndef RGB_FRAME_RENDERER_HPP
#define RGB_FRAME_RENDERER_HPP

#include "convert.h"
#include "logger.h"

extern "C" {
#include <libavutil/frame.h>
#include <libswscale/swscale.h>
#include <libavcodec/avcodec.h>
}

struct RGBFrameRenderer : GlContextRenderer
{
	AVFrame *_out_frame;
	AVPixelFormat _out_format;
	uint8_t *_out_buffer;

	struct SwsContext *_sws_context;
	enum AVPixelFormat _pix_fmt;

	int _frameWidth, _frameHeight;

	RGBFrameRenderer(GlContext *context, int frameWidth, int frameHeight, AVPixelFormat pix_fmt)
	: GlContextRenderer(context), _frameWidth(frameWidth), _frameHeight(frameHeight), _pix_fmt(pix_fmt),
	_sws_context(NULL),
	_out_frame(NULL),
	_out_buffer(NULL)
	{
		_out_frame = av_frame_alloc();
		if (_out_frame == NULL) {
			LOG_ERROR("player_alloc_video_frames failed to allocate frame");
			return;
		}

		int numBytes = avpicture_get_size(PIX_FMT_RGBA, _frameWidth, _frameHeight);

		_out_buffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));
		if (_out_buffer == NULL) {
			LOG_ERROR("player_alloc_video_frames failed to allocate buffer");
			return;
		}

		avpicture_fill((AVPicture *) _out_frame, _out_buffer,
				PIX_FMT_RGBA, _frameWidth, _frameHeight);

		int32_t format = context->getFormat();
		if (format == WINDOW_FORMAT_RGBA_8888) {
			_out_format = PIX_FMT_RGBA;
		} else if (format == WINDOW_FORMAT_RGBX_8888) {
			_out_format = PIX_FMT_RGB0;
			LOG_ERROR("Format: WINDOW_FORMAT_RGBX_8888 (not supported)");
		} else if (format == WINDOW_FORMAT_RGB_565) {
			_out_format = PIX_FMT_RGB565;
			LOG_ERROR("Format: WINDOW_FORMAT_RGB_565 (not supported)");
		} else {
			LOG_ERROR("Unknown window format");
		}

		if (pix_fmt != PIX_FMT_YUV420P && pix_fmt != PIX_FMT_NV12) {
			LOG_WARN("Using slow conversion for non YUV pixel format: %d ", _pix_fmt);
		}

	}

	virtual ~RGBFrameRenderer() {
		if (_out_frame != NULL) {
			av_frame_free(&_out_frame);
			_out_frame = NULL;
		}
	}

	virtual bool bindRGBA(const uint8_t *data, int linesize) = 0;

	virtual bool bindFrame(AVFrame *frame){

		if (_pix_fmt == PIX_FMT_YUV420P) {
			__I420ToARGB(frame->data[0], frame->linesize[0], frame->data[2],
					frame->linesize[2], frame->data[1], frame->linesize[1],
					_out_frame->data[0], _out_frame->linesize[0], _frameWidth,
					_frameHeight);
		} else if (_pix_fmt == PIX_FMT_NV12) {
			__NV21ToARGB(frame->data[0], frame->linesize[0], frame->data[1],
					frame->linesize[1], _out_frame->data[0], _out_frame->linesize[0],
					_frameWidth, _frameHeight);
		} else {
			_sws_context = sws_getCachedContext(_sws_context, _frameWidth, _frameHeight,
					_pix_fmt, _frameWidth, _frameHeight, _out_format,
					SWS_FAST_BILINEAR, NULL, NULL, NULL);
			if (_sws_context == NULL) {
				LOG_ERROR("could not initialize conversion context from: %d"
				", to :%d\n", _pix_fmt, _context->getFormat());
				// TODO some error
				return false;
			}
			sws_scale(_sws_context, (const uint8_t * const *) frame->data,
					frame->linesize, 0, _frameHeight, _out_frame->data,
					_out_frame->linesize);
		}

		return bindRGBA(_out_frame->data[0], _out_frame->linesize[0]);
	}
};


#endif

