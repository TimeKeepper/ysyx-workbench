#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

int vsprintf(char *out, const char *fmt, va_list ap) {
  int count = 0;

  // Loop through format string
  while (*fmt != '\0') {
    if (*fmt == '%') {
      char next = *(fmt + 1);

      if (next == 'd') {
        int num = va_arg(ap, int);
        char buf[32];
        int i = 0;
        if (num == 0) {
          buf[i++] = '0';
        } 
        else {
          if (num < 0) {
            out[count++] = '-';
            num = -num;
          }
          if (num == 0x80000000) {
            buf[i++] = '8';
            num = 214748364;
          }
          while (num != 0) {
            buf[i++] = num % 10 + '0';
            num /= 10;
          }
        }
        while (i > 0) {
          out[count++] = buf[--i];
        }
      } 
      else if (next == 's') {
        char *str = va_arg(ap, char*);;
        while (*str != '\0') {
          out[count++] = *str;
          str++;
        }
      }

      fmt += 2; // Move to next specifier
    } 
    else {
      out[count++] = *fmt;
      fmt++;
    }
  }

  out[count] = '\0'; // Null-terminate the output string

  return count;
}

int sprintf(char *out, const char *fmt, ...) {
  va_list args;
  va_start(args, fmt);
  int ret = vsprintf(out, fmt, args);
  va_end(args);
  return ret;
}

int printf(const char *fmt, ...) {
  char buf[1024];
  va_list args;
  va_start(args, fmt);
  int ret = vsprintf(buf, fmt, args);
  va_end(args);                                                   
  return ret;
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  panic("Not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

#endif
