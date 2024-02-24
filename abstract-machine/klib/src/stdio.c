#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

int printf(const char *fmt, ...) {
  panic("Not implemented");
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  int count = 0;

  // Loop through format string
  while (*fmt != '\0') {
    if (*fmt == '%') {
      char next = *(fmt + 1);

      if (next == 'd') {
        int num = va_arg(ap, int);
        while(num != 0) {
          out[count++] = num % 10 + '0';
          num /= 10;
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

int snprintf(char *out, size_t n, const char *fmt, ...) {
  panic("Not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

#endif
