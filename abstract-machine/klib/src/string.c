#include <klib.h>
#include <klib-macros.h>
#include <stdint.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

size_t strlen(const char *s) {
  size_t len = 0;
  while(*s++) {
    len++;
  }
  return len;
}

char *strcpy(char *dst, const char *src) {
  size_t n = strlen(src);
  return strncpy(dst, src, n);
}

char *strncpy(char *dst, const char *src, size_t n) {
  memcpy(dst, src, n);
  *(dst+n) = '\0';
  return dst;
}

char *strcat(char *dst, const char *src) {
  char *result = dst;
  dst += strlen(dst);
  strcpy(dst, src);
  return result;
}

int strcmp(const char *s1, const char *s2) {
  size_t len_1 = strlen(s1);
  if(len_1 != strlen(s2)) return 1;
  return strncmp(s1, s2, len_1);
}

int strncmp(const char *s1, const char *s2, size_t n) {
  return memcmp(s1, s2, n);
}

void *memset(void *s, int c, size_t n) {
  char *p = s;
  while(n--) {
    *p++ = c;
  }
  return s;
}

void *memmove(void *dst, const void *src, size_t n) {
  char *d = dst;
  if(dst < src) {
    while(n--) {
      *d++ = *(char *)src++;
    }
  } else {
    d += n;
    src += n;
    while(n--) {
      *--d = *(char *)--src;
    }
  }
  return dst;
}

void *memcpy(void *out, const void *in, size_t n) {
  char *dst = out;
  const char *src = in;
  while(n--) {
    *dst++ = *src++;
  }
  return out;
}

int memcmp(const void *s1, const void *s2, size_t n) {
  const uint8_t *p1 = s1, *p2 = s2;
  while(n--) {
    if(*p1 != *p2) return *p1 - *p2;
    p1++;
    p2++;
  }
  return 0;
}

#endif
